package gov.pnnl.gridappsd.cimhub.components;
//	----------------------------------------------------------
//	Copyright (c) 2018, Battelle Memorial Institute
//	All rights reserved.
//	----------------------------------------------------------

import org.apache.jena.query.*;
import java.util.HashMap;

public class DistSolar extends DistComponent {
	public String id;
	public String name;
	public String bus;
	public String phases;
	public double p;
	public double q;
	public double ratedU;
	public double ratedS;
	public double maxIFault;
	public boolean bDelta;

	public String GetJSONEntry () {
		StringBuilder buf = new StringBuilder ();

		buf.append ("{\"name\":\"" + name +"\"");
		buf.append (",\"mRID\":\"" + id +"\"");
		buf.append (",\"CN1\":\"" + bus + "\"");
		buf.append (",\"phases\":\"" + phases + "\"");
		buf.append (",\"ratedS\":" + df1.format(ratedS));
		buf.append (",\"ratedU\":" + df1.format(ratedU));
		buf.append (",\"p\":" + df3.format(p));
		buf.append (",\"q\":" + df3.format(q));
		buf.append (",\"maxIFault\":" + df3.format(maxIFault));
		buf.append ("}");
		return buf.toString();
	}

	public DistSolar (ResultSet results) {
		if (results.hasNext()) {
			QuerySolution soln = results.next();
			name = SafeName (soln.get("?name").toString());
			id = soln.get("?id").toString();
			bus = SafeName (soln.get("?bus").toString());
			phases = OptionalString (soln, "?phases", "ABC");
			phases = phases.replace ('\n', ':');
			p = Double.parseDouble (soln.get("?p").toString());
			q = Double.parseDouble (soln.get("?q").toString());
			ratedU = Double.parseDouble (soln.get("?ratedU").toString());
			ratedS = Double.parseDouble (soln.get("?ratedS").toString());
			maxIFault = Double.parseDouble (soln.get("?ipu").toString());
			bDelta = false;
		}		
//		System.out.println (DisplayString());
	}

	public String DisplayString() {
		StringBuilder buf = new StringBuilder ("");
		buf.append (name + " @ " + bus + " phases=" + phases);
		buf.append (" vnom=" + df4.format(ratedU) + " vanom=" + df4.format(ratedS));
		buf.append (" kw=" + df4.format(0.001 * p) + " kvar=" + df4.format(0.001 * q) + " ilimit=" + df4.format(maxIFault));
		return buf.toString();
	}

	public String GetJSONSymbols(HashMap<String,DistCoordinates> map) {
		DistCoordinates pt = map.get("PhotovoltaicUnit:" + name + ":1");

		StringBuilder buf = new StringBuilder ();

		buf.append ("{\"name\":\"" + name +"\"");
		buf.append (",\"parent\":\"" + bus +"\"");
		buf.append (",\"phases\":\"" + phases +"\"");
		buf.append (",\"kva\":" + df1.format(0.001 * ratedS));
		buf.append (",\"x1\":" + Double.toString(pt.x));
		buf.append (",\"y1\":" + Double.toString(pt.y));
		buf.append ("}");

		return buf.toString();
	}

	public String GetGLM() {
		StringBuilder buf = new StringBuilder("object inverter {\n");

		buf.append ("  name \"inv_pv_" + name + "\";\n");
		buf.append ("  parent \"" + bus + "_pvmtr\";\n");
		if (bDelta && !phases.contains("D")) {
			buf.append ("  phases " + phases.replace (":", "") + "D;\n");
		} else if (!phases.contains("S") && !phases.contains("N")) {
			buf.append ("  phases " + phases.replace (":", "") + "N;\n");
		} else {
			buf.append ("  phases " + phases.replace (":", "") + ";\n");
		}
		buf.append ("  generator_status ONLINE;\n");
		buf.append ("  four_quadrant_control_mode CONSTANT_PQ;\n");
		buf.append ("  inverter_type FOUR_QUADRANT;\n");
		buf.append ("  inverter_efficiency 1.0;\n");
		buf.append ("  power_factor 1.0;\n");
		buf.append ("  V_base " + df3.format (ratedU) + ";\n");
		buf.append ("  rated_power " + df3.format (ratedS) + ";\n");
		buf.append ("  P_Out " + df3.format (p) + ";\n");
		buf.append ("  Q_Out " + df3.format (q) + ";\n");
		buf.append ("  object solar {\n");
		buf.append ("    name \"pv_" + name + "\";\n");
		buf.append ("    generator_mode SUPPLY_DRIVEN;\n");
		buf.append ("    generator_status ONLINE;\n");
		buf.append ("    panel_type SINGLE_CRYSTAL_SILICON;\n");
		buf.append ("    efficiency 0.2;\n");
		buf.append ("    rated_power " + df3.format (ratedS) + ";\n");
		buf.append ("  };\n");

		buf.append("}\n");

		return buf.toString();
	}

	public String GetDSS() {
		StringBuilder buf = new StringBuilder ("new PVSystem." + name);

		int nphases = DSSPhaseCount(phases, bDelta);
		double kv = 0.001 * ratedU;
		double kva = 0.001 * ratedS;
		if (nphases < 2) { // 2-phase wye load should be line-line for secondary?
			kv /= Math.sqrt(3.0);
		}
    double s = Math.sqrt(p*p + q*q);
    double pf = 1.0;
    if (s > 0.0) {
      pf = p / s;
    }
    if (q < 0.0) {
      pf *= -1.0;
    }

//		System.out.println (name + ":" + bus + ":" + Boolean.toString(bDelta) + ":" + phases + ":" + Integer.toString(nphases));

		buf.append (" phases=" + Integer.toString(nphases) + " bus1=" + DSSShuntPhases (bus, phases, bDelta) + 
								" conn=" + DSSConn(bDelta) + " kva=" + df3.format(kva) + " kv=" + df3.format(kv) +
								" pmpp=" + df3.format(kva) + " irrad=" + df3.format(0.001 * p/kva) + " pf=" + df4.format(pf) +
								" vminpu=" + df4.format(1.0/maxIFault) + " LimitCurrent=yes");
		buf.append("\n");

		return buf.toString();
	}

  public static String szCSVHeader = "Name,NumPhases,Bus,Phases,kV,kVA,Connection,kW,pf";

  public String GetCSV () {
    StringBuilder buf = new StringBuilder (name + ",");

    int nphases = DSSPhaseCount(phases, bDelta);
    double kv = 0.001 * ratedU;
    double kva = 0.001 * ratedS;
    if (nphases < 2) { // 2-phase wye load should be line-line for secondary?
      kv /= Math.sqrt(3.0);
    }
    double s = Math.sqrt(p*p + q*q);
    double pf = 1.0;
    if (s > 0.0) {
      pf = p / s;
    }
    if (q < 0.0) {
      pf *= -1.0;
    }

    buf.append (Integer.toString(nphases) + "," + bus + "," + CSVPhaseString (phases) + "," + df3.format(kv) + "," + 
                df3.format(kva) + "," + DSSConn(bDelta) + "," + df3.format(0.001 * p) + "," + df4.format(pf) + "\n");

    return buf.toString();
  }

	public String GetKey() {
		return name;
	}
}

