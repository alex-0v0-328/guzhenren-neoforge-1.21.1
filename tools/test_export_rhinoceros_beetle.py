from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

import export_rhinoceros_beetle as exporter


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def main() -> None:
    model_path = exporter.SOURCE_MODEL
    output_root = Path(__file__).resolve().parents[1]
    before_hashes = exporter.source_hashes(model_path)
    summary = exporter.validate_artifacts(model_path, output_root, before_hashes)
    check(summary == {"animations": 5, "bones": 17, "cubes": 36, "keyframes": 142, "textures": 3}, "Unexpected validation summary")
    geometry = load_json(exporter.output_paths(output_root)[0])["minecraft:geometry"][0]
    bones = {bone["name"]: bone for bone in geometry["bones"]}
    check(bones["elytron_left"]["pivot"] == [-0.5, 0.75, 0.8], "Elytron X pivot was not converted")
    check(bones["elytron_left"]["rotation"] == [45, -16, -10], "Elytron multi-axis rotation was not converted")
    thorax_cube = bones["thorax"]["cubes"][0]
    check(thorax_cube["origin"] == [-1.5, 0.75, -1], "Cube X origin was not converted")
    check(thorax_cube["rotation"] == [22.5, 0, 0], "Cube rotation was not converted")
    wing_cube = bones["wing_left"]["cubes"][0]
    check(wing_cube["size"][1] == 0, "Zero-thickness wing was changed")
    check(wing_cube["rotation"] == [0, 22.5, 0], "Wing Y rotation was not converted")
    for bone in geometry["bones"]:
        for cube in bone.get("cubes", []):
            check(
                set(cube["uv"]) == {"north", "east", "south", "west", "up", "down"},
                "Per-face UV mapping was not emitted under the geometry uv field",
            )
            for direction in ("up", "down"):
                check(all(value <= 0 for value in cube["uv"][direction]["uv_size"]), "Top or bottom UV was not flipped")
    animations = load_json(exporter.output_paths(output_root)[1])["animations"]
    check(animations["animation.idle"]["loop"] is True, "Idle loop flag was not preserved")
    check("loop" not in animations["animation.lift"], "Once animation gained a loop flag")
    check(
        animations["animation.idle"]["bones"]["elytron_left"]["rotation"]["1.5"]["vector"] == [1, 0, 0],
        "Animation rotation X inversion was not preserved",
    )
    check(
        animations["animation.walk"]["bones"]["front_leg_right"]["rotation"]["0.7"]["vector"] == [0, 20, 0],
        "Animation rotation Y inversion was not preserved",
    )
    check(
        animations["animation.lift"]["bones"]["body"]["position"]["0.3"]["vector"] == [0, 0.1, 0],
        "Animation position keyframe was not preserved",
    )
    for animation in animations.values():
        for bone in animation.get("bones", {}).values():
            for channel in bone.values():
                for keyframe in channel.values():
                    check("vector" in keyframe, "Single-point linear keyframe did not use vector")
                    check("post" not in keyframe and "lerp_mode" not in keyframe, "Unexpected multi-point keyframe form")
    after_hashes = exporter.source_hashes(model_path)
    check(after_hashes == before_hashes, "Source hash changed during validation")
    for source_name, target_name in exporter.TEXTURE_MAP.items():
        check(
            sha256(model_path.parent / source_name) == sha256(exporter.output_paths(output_root)[2] / target_name),
            f"Texture hash mismatch: {target_name}",
        )
    print(json.dumps(summary, sort_keys=True))
    print("TEST_EXPORT_RHINOCEROS_BEETLE_OK")


if __name__ == "__main__":
    main()
