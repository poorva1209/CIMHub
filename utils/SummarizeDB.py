from SPARQLWrapper import SPARQLWrapper2
import sys
import re

cim_ns = ''
blz_url = ''
sparql = None

prefix_template = """PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX c: <{cimURL}>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
"""

count_classes = """SELECT ?class (COUNT(?class) as ?cnt)
WHERE {
  ?s a ?class .
} group by ?class order by ?class
"""

count_tuples = """SELECT (COUNT(?s) as ?cnt) where {
  ?s ?attr ?val.
}
"""

def run_query (lbl, qstr):
  print (lbl)
  sparql.setQuery (prefix + qstr)
  ret = sparql.query()
  for b in ret.bindings:
    if 'class' in b:
      print('  {:70s} {:5d}'.format(b['class'].value, int(b['cnt'].value)))
    else:
      print('  {:d} tuples'.format(int(b['cnt'].value)))

if len(sys.argv) < 2:
  print ('usage: python3 SummarizeDB.py db.cfg')
  print (' Blazegraph server must already be started')
  exit()

fp = open (sys.argv[1], 'r')
for ln in fp.readlines():
  toks = re.split('[,\s]+', ln)
  if toks[0] == 'blazegraph_url':
    blz_url = toks[1]
    sparql = SPARQLWrapper2 (blz_url)
    sparql.method = 'POST'
  elif toks[0] == 'cim_namespace':
    cim_ns = toks[1]
    prefix = prefix_template.format(cimURL=cim_ns)
fp.close()

run_query ('Class Summary', count_classes)
run_query ('Tuple Summary', count_tuples)

