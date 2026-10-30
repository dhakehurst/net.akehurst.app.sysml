import re, sys

HEADER = re.compile(r'^([A-Za-z_][A-Za-z0-9_]*)\s*(?::\s*[A-Za-z_][A-Za-z0-9_]*\s*)?=\s*(.*)$')

def parse_rules(path):
    rules = {}
    order = []
    cur = None
    for line in open(path, encoding='utf-8'):
        line = line.rstrip('\n')
        stripped = line.strip()
        if stripped.startswith('//'):
            continue
        m = HEADER.match(line)
        if m and not line.startswith(' ') and not line.startswith('\t'):
            cur = m.group(1)
            if cur not in rules:
                order.append(cur)
            rules[cur] = [m.group(2)]
        elif cur is not None:
            # strip trailing comment
            rules[cur].append(line)
    return {k: ' '.join(v) for k, v in rules.items()}, order

TOKEN = re.compile(r"'(?:\\.|[^'\\])*'|\[[^\]]*\]|\{[^}]*\}|\+=|\?=|=|[A-Za-z_][A-Za-z0-9_]*|\S")

def convert_body(body):
    toks = TOKEN.findall(body)
    out = []
    i = 0
    while i < len(toks):
        t = toks[i]
        if t == '~':
            i += 1; continue   # non-owning reference marker
        if t.startswith('{'):
            i += 1; continue   # semantic action
        if t.startswith('['):
            inner = t[1:-1].strip()
            out.append(inner)   # cross reference
            i += 1; continue
        if re.match(r'[A-Za-z_]', t) and i + 1 < len(toks) and toks[i+1] in ('=', '+=', '?='):
            i += 2; continue   # assignment target
        out.append(t)
        i += 1
    s = ' '.join(out)
    s = s.replace('( )', '').strip()
    return s

def refs(body):
    return set(t for t in TOKEN.findall(body) if re.match(r'[A-Za-z_][A-Za-z0-9_]*$', t))

sysml, sorder = parse_rules(sys.argv[1])
kerml, korder = parse_rules(sys.argv[2])
goal = sys.argv[3]
lexical = set(sys.argv[4].split(','))

conv = {}
for src in (kerml, sysml):
    for k, v in src.items():
        conv[k] = convert_body(v)

import importlib.util, os
spec = importlib.util.spec_from_file_location('fixes', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fixes.py'))
fx = importlib.util.module_from_spec(spec); spec.loader.exec_module(fx)
# whole rules replaced (or added) by hand, already in agl syntax
added = [k for k in fx.OVERRIDES if k not in conv]
conv.update(fx.OVERRIDES)
for k, v in fx.EXTEND.items(): conv[k] = conv[k] + ' | ' + v

# closure from goal, SysML definitions override KerML
todo = [goal]; seen = []
while todo:
    r = todo.pop()
    if r in seen or r in lexical: continue
    if r not in conv:
        print('// MISSING ' + r, file=sys.stderr); continue
    seen.append(r)
    for x in refs(conv[r]):
        if x in conv or x in lexical:
            todo.append(x)

# rules whose body is empty match nothing in the text: remove them and their uses (EmptyResultMember etc.)
empty = set(r for r in seen if not conv[r].strip())
changed = True
while changed:
    changed = False
    for r in seen:
        if r in empty: continue
        b = conv[r]
        nb = ' '.join(t for t in TOKEN.findall(b) if t not in empty)
        nb = re.sub(r"\(\s*\)\s*[?*+]?", '', nb).strip()
        nb = re.sub(r'^\|\s*', '', nb)
        if nb != b:
            conv[r] = nb
        if not conv[r].strip() and r not in empty:
            empty.add(r); changed = True
seen = [r for r in seen if r not in empty]
print('// removed empty rules: ' + ', '.join(sorted(empty)), file=sys.stderr)

def origin(r): return 'SysML' if r in sysml else 'KerML'
ordered = [r for r in sorder if r in seen] + [r for r in korder if r in seen and r not in sysml] + [r for r in added if r in seen]
for r in ordered:
    body = conv[r]
    if not body:
        body = ''
    line = f'    {r} = {body} ;'
    for a, b in fx.FIXES.items():
        if line.startswith(a): line = b + line[len(a):]
    print(line)
