from __future__ import annotations

import argparse
import hashlib
import json
import math
import shutil
from pathlib import Path
from typing import Any


SOURCE_MODEL = Path(
    r"C:\alex\code\blockbench\gzr-models\geckolib-rhinoceros-beetle\geckolib-rhinoceros-beetle.bbmodel"
)
TEXTURE_MAP = {
    "texture_light.png": "horizontal_crash_gu.png",
    "texture.png": "vertical_crash_gu.png",
    "texture_dark.png": "charging_crash_gu.png",
}
MODEL_FORMAT_VERSION = "1.12.0"
ANIMATION_FORMAT_VERSION = "1.8.0"
CHANNELS = ("position", "rotation", "scale")


def fail(message: str) -> None:
    raise ValueError(message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def source_hashes(model_path: Path) -> dict[str, str]:
    paths = [model_path, *(model_path.parent / name for name in TEXTURE_MAP)]
    return {path.name: file_hash(path) for path in paths}


def load_model(model_path: Path) -> dict[str, Any]:
    require(model_path.is_file(), f"Missing Blockbench model: {model_path}")
    with model_path.open("r", encoding="utf-8") as handle:
        model = json.load(handle)
    require(model.get("meta", {}).get("model_format") == "geckolib_model", "Unexpected model format")
    require(model.get("meta", {}).get("format_version") == "5.0", "Unexpected Blockbench format version")
    require(model.get("geckolib_model_type") == "Entity", "Expected an Entity GeckoLib model")
    require(model.get("model_identifier") == "rhinoceros_beetle", "Unexpected model identifier")
    require(model.get("geckolib_modid") == "guzhenren", "Unexpected GeckoLib mod id")
    resolution = model.get("resolution", {})
    require(resolution.get("width") == 64 and resolution.get("height") == 64, "Expected a 64x64 model")
    return model


def number(value: Any, label: str) -> int | float:
    if isinstance(value, bool):
        fail(f"Boolean is not numeric in {label}")
    if isinstance(value, (int, float)):
        result = value
    elif isinstance(value, str):
        text = value.strip()
        if not text:
            return 0
        try:
            result = float(text)
        except ValueError as error:
            raise ValueError(f"Unsupported nonnumeric Molang in {label}: {value!r}") from error
    else:
        fail(f"Unsupported numeric value in {label}: {value!r}")
    if not math.isfinite(result):
        fail(f"Nonfinite numeric value in {label}")
    if result == 0:
        return 0
    if isinstance(result, float) and result.is_integer():
        return int(result)
    return result


def vector3(values: Any, label: str) -> list[int | float]:
    require(isinstance(values, list) and len(values) == 3, f"Expected a three-axis vector in {label}")
    return [number(value, f"{label}[{index}]") for index, value in enumerate(values)]


def vector3_difference(left: list[int | float], right: list[int | float]) -> list[int | float]:
    return [left[index] - right[index] for index in range(3)]


def flip_x(values: list[int | float]) -> list[int | float]:
    return [-values[0], values[1], values[2]]


def flip_rotation(values: list[int | float]) -> list[int | float]:
    return [-values[0], -values[1], values[2]]


def is_zero_vector(values: list[int | float]) -> bool:
    return all(value == 0 for value in values)


def node_uuid(node: Any, label: str) -> str:
    if isinstance(node, str):
        return node
    require(isinstance(node, dict) and isinstance(node.get("uuid"), str), f"Invalid outliner node in {label}")
    return node["uuid"]


def maps(model: dict[str, Any]) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    groups = {group["uuid"]: group for group in model.get("groups", [])}
    elements = {element["uuid"]: element for element in model.get("elements", [])}
    require(len(groups) == len(model.get("groups", [])), "Duplicate group UUID")
    require(len(elements) == len(model.get("elements", [])), "Duplicate element UUID")
    names = [group.get("name") for group in groups.values()]
    require(all(isinstance(name, str) for name in names), "Every group needs a name")
    require(len(names) == len(set(names)), "Duplicate group name")
    return groups, elements


def compile_face(face: dict[str, Any], label: str) -> dict[str, Any]:
    require(face.get("texture") is not None, f"Untextured face in {label}")
    values = face.get("uv")
    require(isinstance(values, list) and len(values) == 4, f"Expected four UV values in {label}")
    uv = [number(value, f"{label}.uv[{index}]") for index, value in enumerate(values)]
    size = [uv[2] - uv[0], uv[3] - uv[1]]
    result: dict[str, Any] = {"uv": uv[:2], "uv_size": size}
    if label.rsplit(".", 1)[-1] in {"up", "down"}:
        result["uv"][0] += result["uv_size"][0]
        result["uv"][1] += result["uv_size"][1]
        result["uv_size"] = [-result["uv_size"][0], -result["uv_size"][1]]
    if face.get("rotation"):
        result["uv_rotation"] = number(face["rotation"], f"{label}.rotation")
    if face.get("material_name"):
        result["material_instance"] = face["material_name"]
    return result


def compile_cube(element: dict[str, Any]) -> dict[str, Any]:
    name = element.get("name", element.get("uuid", "cube"))
    from_point = vector3(element.get("from"), f"{name}.from")
    to_point = vector3(element.get("to"), f"{name}.to")
    size = vector3_difference(to_point, from_point)
    result: dict[str, Any] = {
        "origin": [-from_point[0] - size[0], from_point[1], from_point[2]],
        "size": size,
    }
    rotation = vector3(element.get("rotation", [0, 0, 0]), f"{name}.rotation")
    if not is_zero_vector(rotation):
        result["pivot"] = flip_x(vector3(element.get("origin"), f"{name}.origin"))
        result["rotation"] = flip_rotation(rotation)
    inflate = element.get("inflate")
    if inflate is not None and number(inflate, f"{name}.inflate") != 0:
        result["inflate"] = number(inflate, f"{name}.inflate")
    if element.get("box_uv"):
        result["uv"] = vector3(element.get("uv_offset", [0, 0]), f"{name}.uv_offset")[:2]
    else:
        faces = element.get("faces", {})
        # Geometry 1.12.0 stores the six directional mappings in the cube's
        # `uv` object.  `faces` is Blockbench's source-model field only.
        result["uv"] = {
            direction: compile_face(face, f"{name}.{direction}")
            for direction, face in faces.items()
            if face.get("texture") is not None
        }
    return result


def compile_geometry(model: dict[str, Any]) -> dict[str, Any]:
    groups, elements = maps(model)
    bones: list[dict[str, Any]] = []
    seen_groups: set[str] = set()
    seen_elements: set[str] = set()

    def compile_group(node: Any, parent: str | None) -> None:
        uuid = node_uuid(node, "group")
        require(uuid in groups, f"Unknown group UUID in outliner: {uuid}")
        require(uuid not in seen_groups, f"Group appears more than once in outliner: {uuid}")
        group = groups[uuid]
        require(group.get("export", True), f"Nonexported group is unsupported: {group['name']}")
        seen_groups.add(uuid)
        name = group["name"]
        bone: dict[str, Any] = {"name": name}
        if parent is not None:
            bone["parent"] = parent
        bone["pivot"] = flip_x(vector3(group.get("origin"), f"{name}.origin"))
        rotation = vector3(group.get("rotation", [0, 0, 0]), f"{name}.rotation")
        if not is_zero_vector(rotation):
            bone["rotation"] = flip_rotation(rotation)
        direct_cubes: list[dict[str, Any]] = []
        child_groups: list[Any] = []
        for child in node.get("children", []) if isinstance(node, dict) else []:
            child_id = node_uuid(child, name)
            if child_id in elements:
                require(child_id not in seen_elements, f"Element appears more than once in outliner: {child_id}")
                element = elements[child_id]
                require(element.get("export", True), f"Nonexported element is unsupported: {element['name']}")
                seen_elements.add(child_id)
                direct_cubes.append(compile_cube(element))
            elif child_id in groups:
                child_groups.append(child)
            else:
                fail(f"Unknown UUID in outliner: {child_id}")
        if direct_cubes:
            bone["cubes"] = direct_cubes
        bones.append(bone)
        for child_group in child_groups:
            compile_group(child_group, name)

    for root in model.get("outliner", []):
        compile_group(root, None)
    require(seen_groups == set(groups), "Outliner does not contain exactly the model groups")
    require(seen_elements == set(elements), "Outliner does not contain exactly the model elements")
    visible_box = model.get("visible_box")
    require(isinstance(visible_box, list) and len(visible_box) == 3, "Expected Blockbench visible_box metadata")
    visible = [number(value, f"visible_box[{index}]") for index, value in enumerate(visible_box)]
    description = {
        "identifier": "geometry.rhinoceros_beetle",
        "texture_width": model["resolution"]["width"],
        "texture_height": model["resolution"]["height"],
        "visible_bounds_width": visible[0],
        "visible_bounds_height": visible[1],
        "visible_bounds_offset": [0, visible[2], 0],
    }
    return {
        "format_version": MODEL_FORMAT_VERSION,
        "minecraft:geometry": [{"description": description, "bones": bones}],
    }


def transform_keyframe_point(point: dict[str, Any], channel: str, label: str) -> list[int | float]:
    require(isinstance(point, dict), f"Invalid keyframe point in {label}")
    values = [number(point.get(axis, 0), f"{label}.{axis}") for axis in ("x", "y", "z")]
    if channel in {"position", "rotation"}:
        values[0] = -values[0]
    if channel == "rotation":
        values[1] = -values[1]
    return values


def compile_keyframe(keyframe: dict[str, Any], channel: str, label: str) -> dict[str, Any]:
    require(keyframe.get("interpolation", "linear") == "linear", f"Unsupported interpolation in {label}")
    require(not keyframe.get("bezier"), f"Unsupported Bezier keyframe in {label}")
    points = keyframe.get("data_points")
    require(isinstance(points, list) and len(points) == 1, f"Expected one linear point in {label}")
    result: dict[str, Any] = {"vector": transform_keyframe_point(points[0], channel, label)}
    easing = keyframe.get("easing")
    if easing:
        result["easing"] = easing
    easing_args = keyframe.get("easingArgs")
    if easing_args is not None:
        require(isinstance(easing_args, list), f"Invalid easing arguments in {label}")
        result["easingArgs"] = easing_args
    return result


def timecode(value: Any) -> str:
    value_number = number(value, "keyframe.time")
    rounded = round(float(value_number), 4)
    if rounded == 0:
        return "0.0"
    if rounded.is_integer():
        return f"{int(rounded)}.0"
    return f"{rounded:.4f}".rstrip("0").rstrip(".")


def compile_animations(model: dict[str, Any]) -> dict[str, Any]:
    groups, _ = maps(model)
    animations: dict[str, Any] = {}
    for animation in model.get("animations", []):
        name = animation.get("name")
        require(isinstance(name, str), "Animation is missing a name")
        result: dict[str, Any] = {}
        loop = animation.get("loop")
        if loop == "loop":
            result["loop"] = True
        elif loop == "hold":
            result["loop"] = "hold_on_last_frame"
        elif loop not in {None, "once"}:
            fail(f"Unsupported animation loop in {name}: {loop!r}")
        if animation.get("length"):
            result["animation_length"] = round(number(animation["length"], f"{name}.length"), 4)
        result["bones"] = {}
        for animator_uuid, animator in animation.get("animators", {}).items():
            keyframes = animator.get("keyframes", [])
            if not keyframes:
                continue
            require(animator.get("type") == "bone", f"Unsupported animator type in {name}: {animator_uuid}")
            require(animator_uuid in groups, f"Animation references unknown group UUID: {animator_uuid}")
            bone_name = groups[animator_uuid]["name"]
            bone_result: dict[str, Any] = {}
            for channel in CHANNELS:
                channel_keyframes = [keyframe for keyframe in keyframes if keyframe.get("channel") == channel]
                if not channel_keyframes:
                    continue
                channel_keyframes.sort(key=lambda keyframe: float(number(keyframe.get("time", 0), "keyframe.time")))
                compiled = {}
                for index, keyframe in enumerate(channel_keyframes):
                    compiled[timecode(keyframe.get("time", 0))] = compile_keyframe(
                        keyframe, channel, f"{name}.{bone_name}.{channel}[{index}]"
                    )
                bone_result[channel] = compiled
            require(bone_result, f"Animation animator has no supported channels: {name}.{bone_name}")
            result["bones"][bone_name] = bone_result
        if not result["bones"]:
            del result["bones"]
        animations[name] = result
    return {"format_version": ANIMATION_FORMAT_VERSION, "animations": animations}


def output_paths(output_root: Path) -> tuple[Path, Path, Path]:
    asset_root = output_root / "src" / "main" / "resources" / "assets" / "guzhenren"
    return (
        asset_root / "geo" / "entity" / "rhinoceros_beetle.geo.json",
        asset_root / "animations" / "entity" / "rhinoceros_beetle.animation.json",
        asset_root / "textures" / "entity",
    )


def write_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f"{path.name}.tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False, allow_nan=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def png_dimensions(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", f"Not a PNG: {path}")
    require(data[12:16] == b"IHDR", f"PNG has no IHDR: {path}")
    return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")


def validate_artifacts(model_path: Path, output_root: Path, before_hashes: dict[str, str] | None = None) -> dict[str, int]:
    model = load_model(model_path)
    hashes = source_hashes(model_path)
    if before_hashes is not None:
        require(hashes == before_hashes, "Source model or texture hash changed during export")
    geo_path, animation_path, texture_root = output_paths(output_root)
    require(geo_path.is_file(), f"Missing geometry output: {geo_path}")
    require(animation_path.is_file(), f"Missing animation output: {animation_path}")
    with geo_path.open("r", encoding="utf-8") as handle:
        geometry = json.load(handle)
    with animation_path.open("r", encoding="utf-8") as handle:
        animations = json.load(handle)
    expected_geometry = compile_geometry(model)
    expected_animations = compile_animations(model)
    require(geometry == expected_geometry, "Geometry output differs from the source export semantics")
    require(animations == expected_animations, "Animation output differs from the source export semantics")
    geometry_entry = geometry["minecraft:geometry"][0]
    require(geometry_entry["description"]["identifier"] == "geometry.rhinoceros_beetle", "Wrong geometry identifier")
    require(len(geometry_entry["bones"]) == len(model["groups"]) == 17, "Unexpected bone count")
    cube_count = sum(len(bone.get("cubes", [])) for bone in geometry_entry["bones"])
    require(cube_count == len(model["elements"]) == 36, "Unexpected cube count")
    require(list(animations["animations"]) == [animation["name"] for animation in model["animations"]], "Animation order changed")
    keyframe_count = sum(
        len(keyframes)
        for animation in animations["animations"].values()
        for bone in animation.get("bones", {}).values()
        for keyframes in bone.values()
        if isinstance(keyframes, dict) and all(isinstance(value, dict) for value in keyframes.values())
    )
    require(keyframe_count == 142, f"Unexpected keyframe count: {keyframe_count}")
    texture_count = 0
    for source_name, target_name in TEXTURE_MAP.items():
        source_path = model_path.parent / source_name
        target_path = texture_root / target_name
        require(target_path.is_file(), f"Missing texture output: {target_path}")
        require(file_hash(source_path) == file_hash(target_path), f"Texture bytes changed for {target_name}")
        require(png_dimensions(source_path) == (64, 64), f"Unexpected source texture dimensions: {source_path}")
        require(png_dimensions(target_path) == (64, 64), f"Unexpected output texture dimensions: {target_path}")
        texture_count += 1
    return {"bones": len(geometry_entry["bones"]), "cubes": cube_count, "animations": len(animations["animations"]), "keyframes": keyframe_count, "textures": texture_count}


def export_project(model_path: Path, output_root: Path) -> dict[str, int]:
    model = load_model(model_path)
    before_hashes = source_hashes(model_path)
    geometry_path, animation_path, texture_root = output_paths(output_root)
    write_json(geometry_path, compile_geometry(model))
    write_json(animation_path, compile_animations(model))
    texture_root.mkdir(parents=True, exist_ok=True)
    for source_name, target_name in TEXTURE_MAP.items():
        source_path = model_path.parent / source_name
        require(source_path.is_file(), f"Missing source texture: {source_path}")
        temporary = texture_root / f"{target_name}.tmp"
        shutil.copyfile(source_path, temporary)
        temporary.replace(texture_root / target_name)
    return validate_artifacts(model_path, output_root, before_hashes)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=SOURCE_MODEL)
    parser.add_argument("--output-root", type=Path, default=Path(__file__).resolve().parents[1])
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--export", action="store_true")
    modes.add_argument("--validate", action="store_true")
    return parser.parse_args()


def main() -> None:
    arguments = parse_args()
    if arguments.export:
        summary = export_project(arguments.source, arguments.output_root)
    else:
        summary = validate_artifacts(arguments.source, arguments.output_root)
    print(json.dumps(summary, sort_keys=True))
    print("VALIDATION_OK")


if __name__ == "__main__":
    main()
