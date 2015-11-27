package org.neuroml2.export.info;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lemsml.model.Case;
import org.lemsml.model.ConditionalDerivedVariable;
import org.lemsml.model.exceptions.LEMSCompilerException;
import org.lemsml.model.extended.LemsNode;
import org.lemsml.model.extended.Scope;
import org.lemsml.model.extended.Symbol;
import org.neuroml2.model.BaseGate;
import org.neuroml2.model.BaseVoltageDepRate;
import org.neuroml2.model.BaseVoltageDepTime;
import org.neuroml2.model.BaseVoltageDepVariable;
import org.neuroml2.model.Cell;
import org.neuroml2.model.GateHHrates;
import org.neuroml2.model.GateHHratesInf;
import org.neuroml2.model.GateHHratesTau;
import org.neuroml2.model.GateHHratesTauInf;
import org.neuroml2.model.GateHHtauInf;
import org.neuroml2.model.IonChannelHH;
import org.neuroml2.model.NeuroML2ModelReader;
import org.neuroml2.model.Neuroml2;

import com.google.common.base.Joiner;

import expr_parser.utils.ExpressionParser;
import expr_parser.utils.UndefinedSymbolException;
import expr_parser.visitors.ARenderAs;
import expr_parser.visitors.AntlrExpressionParser;
import expr_parser.visitors.RenderMathJS;

public class InfoExtractorTest<V> {

	private Neuroml2 hh;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Throwable {
		hh = NeuroML2ModelReader
				.read(getLocalFile("/NML2_SingleCompHHCell.nml"));
	}

//	@Test
	public void channelRatesBAD() throws LEMSCompilerException {
		for (Cell cell : hh.getCells()) {
			for (IonChannelHH chan : cell.getAllOfType(IonChannelHH.class)) {
				for (BaseGate gate : chan.getAllOfType(BaseGate.class)) {
					System.out.println(gateInfoBAD(gate));
				}
			}
		}
	}

	@Test
	public void channelRatesGood() throws LEMSCompilerException,
			UndefinedSymbolException {
		for (Cell cell : hh.getCells()) {
			for (IonChannelHH chan : cell.getAllOfType(IonChannelHH.class)) {
				System.out.println("#############################");
				System.out.println("channel:" + chan.getName());
				for (BaseGate gate : chan.getAllOfType(BaseGate.class)) {
					System.out.println("gate:" + gate.getName());
					System.out.println(gateInfoGood(gate));
				}
				System.out.println("#############################\n\n");
			}
		}
	}

	private String gateInfoGood(BaseGate gate) throws LEMSCompilerException,
			UndefinedSymbolException {
		String ret = "";
		for (BaseVoltageDepRate r : gate.getAllOfType(BaseVoltageDepRate.class)) {
			ret += r.getName() + ": \n"
					+ processExpression(r.getScope().resolve("r"));
		}
		for (BaseVoltageDepTime t : gate.getAllOfType(BaseVoltageDepTime.class)) {
			ret += t.getName() + ": \n"
					+ processExpression(t.getScope().resolve("t"));
		}
		for (BaseVoltageDepVariable x : gate
				.getAllOfType(BaseVoltageDepVariable.class)) {
			ret += x.getName() + ": \n"
					+ processExpression(x.getScope().resolve("x"));
		}

		return ret;
	}

	private String processExpression(Symbol resolved)
			throws LEMSCompilerException, UndefinedSymbolException {

		LemsNode type = resolved.getType();
		FunctionNode f = new FunctionNode();
		f.setName(resolved.getName());
		f.register(depsToMathJS(resolved));
		f.setIndependentVariable("v");
		f.deRegister("v"); // redundant v=v

		if (type instanceof ConditionalDerivedVariable) {
			f.deRegister(resolved.getName()); // redundant x=x
			ConditionalDerivedVariable cdv = (ConditionalDerivedVariable) resolved.getType();
			f.register(f.getName(), conditionalDVToMathJS(cdv));
		}

		return f.toString() +"\n";
	}

	public Set<String> findIndependentVariables(String expression,
			Map<String, String> context) {
		Set<String> vars = ExpressionParser.listSymbolsInExpression(expression);
		vars.removeAll(context.keySet());
		return vars;
	}

	private String conditionalDVToMathJS(ConditionalDerivedVariable cdv) {
		List<String> condsVals = new ArrayList<String>();
		String defaultCase = null;

		for (Case c : cdv.getCase()) {
			if (null == c.getCondition()) // undocumented LEMS feature: no
											// condition, "catch-all" case
				defaultCase = adaptToMathJS(c.getValueDefinition());
			else
				condsVals.add(adaptToMathJS(c.getCondition()) + " ? "
						+ adaptToMathJS(c.getValueDefinition()));
		}
		if (null != defaultCase)
			condsVals.add(defaultCase);
		else
			condsVals.add("null"); // no case satisfied, no default

		return Joiner.on(" : ").join(condsVals);
	}

	private Map<String, String> depsToMathJS(Symbol resolved)
			throws LEMSCompilerException, UndefinedSymbolException {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		Scope scope = resolved.getScope();
		Map<String, String> sortedContext = scope.buildSortedContext(resolved);
		for(Entry<String, String> kv : sortedContext.entrySet()){
			String var = kv.getKey();
			String def = kv.getValue();
			ret.put(var, adaptToMathJS(def));
		}
		return ret;
	}

	private String adaptToMathJS(String expression) {
		ARenderAs adaptor = new RenderMathJS(hh.getSymbolToUnit());
		AntlrExpressionParser p = new AntlrExpressionParser(expression);
		return p.parseAndVisitWith(adaptor);
	}

	private String gateInfoBAD(BaseGate gate) throws LEMSCompilerException {
		String ret = "";
		if (gate instanceof GateHHrates) {
			GateHHrates g = (GateHHrates) gate;
			ret += MessageFormat.format("rev [{0}]:", g.getComponentType()
					.getName());
			ret += g.getReverseRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("fwd [{0}]:", g.getComponentType()
					.getName());
			ret += g.getForwardRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";
		} else if (gate instanceof GateHHratesInf) {
			GateHHratesInf g = (GateHHratesInf) gate;
			ret += MessageFormat.format("reverse [{0}]:", g.getComponentType()
					.getName());
			ret += g.getReverseRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("fwd [{0}]:", g.getComponentType()
					.getName());
			ret += g.getForwardRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("inf [{0}]:", g.getComponentType()
					.getName());
			ret += g.getSteadyState().getScope().resolve("x")
					.getValueDefinition()
					+ "\n";

		} else if (gate instanceof GateHHratesTau) {
			GateHHratesTau g = (GateHHratesTau) gate;
			ret += MessageFormat.format("rev [{0}]:", g.getComponentType()
					.getName());
			ret += g.getReverseRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("inf [{0}]:", g.getComponentType()
					.getName());
			ret += g.getForwardRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("tau [{0}]:", g.getComponentType()
					.getName());
			ret += g.getTimeCourse().getScope().resolve("t")
					.getValueDefinition()
					+ "\n";
		} else if (gate instanceof GateHHratesTauInf) {
			GateHHratesTauInf g = (GateHHratesTauInf) gate;
			ret += MessageFormat.format("rev [{0}]:", g.getComponentType()
					.getName());
			ret += g.getReverseRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("rev [{0}]:", g.getComponentType()
					.getName());
			ret += g.getForwardRate().getScope().resolve("r")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("tau [{0}]:", g.getComponentType()
					.getName());
			ret += g.getTimeCourse().getScope().resolve("t")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("inf [{0}]:", g.getComponentType()
					.getName());
			ret += g.getSteadyState().getScope().resolve("x")
					.getValueDefinition()
					+ "\n";
		} else if (gate instanceof GateHHtauInf) {
			GateHHtauInf g = (GateHHtauInf) gate;

			ret += MessageFormat.format("tau [{0}]:", g.getComponentType()
					.getName());
			ret += g.getTimeCourse().getScope().resolve("t")
					.getValueDefinition()
					+ "\n";

			ret += MessageFormat.format("inf [{0}]:", g.getComponentType()
					.getName());
			ret += g.getSteadyState().getScope().resolve("x")
					.getValueDefinition()
					+ "\n";
		}

		return ret;
	}

	protected File getLocalFile(String fname) {
		return new File(getClass().getResource(fname).getFile());
	}

}
