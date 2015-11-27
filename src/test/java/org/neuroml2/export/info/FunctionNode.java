package org.neuroml2.export.info;

import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionNode {
	private String name;
	private String independentVariable;
	private Double[] xRange;
	private Double deltaX;
	Map<String, String> context = new LinkedHashMap<String, String>();

	public void setIndependentVariable(String x) {
		this.independentVariable = x;
	}

	public String getExpression() {
		return context.get(getName());
	}

	public void register(String variable, String value) {
		this.context.put(variable, value);
	}

	public void register(Map<String, String> ctxt) {
		this.context.putAll(ctxt);
	}

	public void deRegister(String k) {
		this.context.remove(k);
	}

	String getIndependentVariable() {
		return independentVariable;
	}

	public Double getDeltaX() {
		return deltaX;
	}

	public void setDeltaX(Double deltaX) {
		this.deltaX = deltaX;
	}

	public Double[] getxRange() {
		return xRange;
	}

	public void setxRange(Double[] xRange) {
		this.xRange = xRange;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return this.getName()  + ": " + this.context;
	}
}
