# Small Kotlin lexical scanner for static checks; this is NOT a Kotlin compiler.
def scan(text):
 tokens=[]; errors=[]
 n=len(text)
 def code(i, until=None):
  stack=[]
  while i<n:
   if text.startswith('//',i):
    j=text.find('\n',i);i=n if j<0 else j+1;continue
   if text.startswith('/*',i):
    start=i;level=1;i+=2
    while i<n and level:
     if text.startswith('/*',i):level+=1;i+=2
     elif text.startswith('*/',i):level-=1;i+=2
     else:i+=1
    if level: errors.append((start,'unclosed comment'))
    continue
   c=text[i]
   if c=='"': i=string(i);continue
   if c=="'":
    start=i;i+=1
    while i<n and text[i]!="'":i+=2 if text[i]=='\\' else 1
    if i>=n:errors.append((start,'unclosed char'))
    i+=1;continue
   if c in '({[':stack.append((c,i))
   elif c in ')}]':
    if until and not stack and c==until:return i+1
    if not stack or stack[-1][0] != {')':'(',']':'[','}':'{'}[c]:errors.append((i,'unmatched '+c))
    else: stack.pop()
   i+=1
  for _,pos in stack: errors.append((pos,'unclosed bracket'))
  if until:errors.append((i,'unclosed template'))
  return i
 def string(i):
  start=i; triple=text.startswith('"""',i);i+=3 if triple else 1
  while i<n:
   if text.startswith('"""' if triple else '"',i):
    i+=3 if triple else 1;tokens.append((start,i,text[start:i]));return i
   if not triple and text[i]=='\\':i+=2;continue
   if text.startswith('${',i):i=code(i+2,'}');continue
   if not triple and text[i]=='\n':errors.append((i,'newline in string'));return i+1
   i+=1
  errors.append((start,'unclosed string'));return i
 code(0)
 return sorted(tokens),errors
