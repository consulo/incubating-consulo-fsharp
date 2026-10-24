import os, re, sys
src, dst = sys.argv[1], sys.argv[2]
os.makedirs(dst, exist_ok=True)
bad = []
for name in sorted(os.listdir(src)):
    out = []
    ok = False
    for line in open(os.path.join(src, name), encoding='utf-8'):
        line = line.strip()
        m = re.match(r'^([A-Z_0-9]+) \((-?\d+),(-?\d+)--(-?\d+),(-?\d+)\)$', line)
        if not m:
            if line.startswith('error'):
                bad.append(name)
            continue
        tok = m.group(1)
        if tok.endswith('_COMING_SOON'):
            continue
        if tok.endswith('_IS_HERE'):
            tok = tok[:-len('_IS_HERE')]
        out.append('%s (%s,%s)' % (tok, m.group(2), m.group(3)))
        if tok == 'EOF':
            ok = True
    if not ok:
        bad.append(name)
        continue
    open(os.path.join(dst, name[:-len('.txt')] + '.lexfilter'), 'w').write('\n'.join(out) + '\n')
print('bad', bad)
