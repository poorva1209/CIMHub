package gov.pnnl.gridappsd.cimhub.components;
//	----------------------------------------------------------
//	Copyright (c) 2017, Battelle Memorial Institute
//	All rights reserved.
//	----------------------------------------------------------

import org.apache.jena.query.*;

public class DistBaseVoltage extends DistComponent {
	public String name;
	double vnom;

	public String GetJSONEntry () {
		StringBuilder buf = new StringBuilder ();

		buf.append ("{\"name\":\"" + name +"\"");
		buf.append ("}");
		return buf.toString();
	}

	public DistBaseVoltage (ResultSet results) {
		if (results.hasNext()) {
			QuerySolution soln = results.next();
			name = soln.get("?vnom").toString();
			vnom = Double.parseDouble (soln.get("?vnom").toString());
		}
	}

	public String DisplayString() {
		StringBuilder buf = new StringBuilder ("");
		buf.append ("vnom=" + df4.format(vnom));
		return buf.toString();
	}

	public String GetKey() {
		return name;
	}

	public String GetDSS() {
		StringBuilder buf = new StringBuilder ("");
		buf.append (df3.format(0.001 * vnom) + " ");
		return buf.toString();
	}
}

