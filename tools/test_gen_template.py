import gzip
from pathlib import Path

import gen_template


def main() -> None:
    repository_root = Path(__file__).resolve().parents[1]
    assert gen_template.REPOSITORY_ROOT == repository_root

    width, height, length = 9, 9, 9
    payload = b"\x0a" + gen_template.nb_name("") + gen_template.build_all_air(width, height, length)
    result = gen_template.verify(gzip.compress(payload), width, height, length)

    assert result["size"] == [width, height, length]
    assert len(result["blocks"]) == width * height * length
    assert result["palette"] == [{"Name": "minecraft:air"}]
    assert result["entities"] == []
    print("gen_template path and NBT semantics verified")


if __name__ == "__main__":
    main()
