import gzip
import struct
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]


def nb_name(name):
    return struct.pack('>H', len(name)) + name.encode('utf-8')


def p_int(v):
    return struct.pack('>i', v)


def p_str(v):
    return struct.pack('>H', len(v)) + v.encode('utf-8')


def p_list_int(vals):
    return bytes([3]) + struct.pack('>i', len(vals)) + b''.join(p_int(v) for v in vals)


def p_list_comp(items):
    return bytes([10]) + struct.pack('>i', len(items)) + b''.join(items)


def entry(name, tag_byte, payload):
    return bytes([tag_byte]) + nb_name(name) + payload


def comp_payload(fields):
    return b''.join(entry(n, t, p) for n, t, p in fields) + b'\x00'


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def take(self, n):
        b = self.d[self.i:self.i + n]
        if len(b) < n:
            raise EOFError('short read at %d' % self.i)
        self.i += n
        return b

    def byte(self):
        return self.take(1)[0]

    def short(self):
        return struct.unpack('>H', self.take(2))[0]

    def i32(self):
        return struct.unpack('>i', self.take(4))[0]

    def string(self):
        return self.take(self.short()).decode('utf-8')

    def payload(self, t):
        if t == 3:
            return self.i32()
        if t == 8:
            return self.string()
        if t == 9:
            et = self.byte()
            n = self.i32()
            return [self.payload(et) for _ in range(n)]
        if t == 10:
            return self.compound()
        raise ValueError('unexpected tag %d at %d' % (t, self.i))

    def compound(self):
        out = {}
        while True:
            t = self.byte()
            if t == 0:
                return out
            name = self.string()
            out[name] = self.payload(t)


def build_all_air(w, h, l):
    blocks = [comp_payload([
        ('pos', 9, p_list_int([x, y, z])),
        ('state', 3, p_int(0)),
    ]) for x in range(w) for y in range(h) for z in range(l)]
    return comp_payload([
        ('size', 9, p_list_int([w, h, l])),
        ('entities', 9, p_list_comp([])),
        ('blocks', 9, p_list_comp(blocks)),
        ('palette', 9, p_list_comp([comp_payload([('Name', 8, p_str('minecraft:air'))])])),
        ('DataVersion', 3, p_int(3955)),
    ])


def verify(blob, w, h, l):
    p = Reader(gzip.decompress(blob))
    assert p.byte() == 10, 'root must be compound'
    assert p.string() == '', 'root must be unnamed'
    r = p.compound()
    assert r['size'] == [w, h, l], r['size']
    assert len(r['blocks']) == w * h * l
    assert r['DataVersion'] == 3955
    assert r['palette'] == [{'Name': 'minecraft:air'}]
    assert r['entities'] == []
    assert r['blocks'][0] == {'pos': [0, 0, 0], 'state': 0}
    assert p.i == len(gzip.decompress(blob)), 'trailing bytes'
    return r


def main():
    sizes = [(9, 9, 9)]
    for (w, h, l) in sizes:
        blob = gzip.compress(b'\x0a' + nb_name('') + build_all_air(w, h, l))
        verify(blob, w, h, l)
        out = REPOSITORY_ROOT / ('src/main/resources/data/guzhenren/structure/empty%dx%dx%d.nbt' % (w, h, l))
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(blob)
        print('written + verified:', out, out.stat().st_size, 'bytes')


if __name__ == '__main__':
    main()
