from SPARQLWrapper import SPARQLWrapper2
import sys

import CIMHubConfig
if len(sys.argv) > 1:
  CIMHubConfig.ConfigFromJsonFile (sys.argv[1])

sparql = SPARQLWrapper2(CIMHubConfig.blazegraph_url)

sparql.setQuery(CIMHubConfig.prefix + 
    """
    SELECT ?feeder ?fid ?station ?sid ?subregion ?sgrid ?region ?rgnid WHERE {
     ?s r:type c:Feeder.
     ?s c:IdentifiedObject.name ?feeder.
     ?s c:IdentifiedObject.mRID ?fid.
     ?s c:Feeder.NormalEnergizingSubstation ?sub.
     ?sub c:IdentifiedObject.name ?station.
     ?sub c:IdentifiedObject.mRID ?sid.
     ?sub c:Substation.Region ?sgr.
     ?sgr c:IdentifiedObject.name ?subregion.
     ?sgr c:IdentifiedObject.mRID ?sgrid.
     ?sgr c:SubGeographicalRegion.Region ?rgn.
     ?rgn c:IdentifiedObject.name ?region.
     ?rgn c:IdentifiedObject.mRID ?rgnid.
    }
    ORDER by ?station ?feeder
""")

ret = sparql.query()
#print ('binding keys are:',ret.variables)
print ('Feeder names and mRIDs:')
for b in ret.bindings:
  print (b['feeder'].value,b['fid'].value)
