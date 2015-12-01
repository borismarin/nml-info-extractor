package org.neuroml2.export.info;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

import expr_parser.visitors.AntlrExpressionParser;
import expr_parser.visitors.ExpandSymbols;


public class SymbolExpander {
	static final LinkedHashMap<String, String> expandSymbols(LinkedHashMap<String, String> context, Queue<String> queue) {
		// we expect a toposorted context, ergo linkedHM
		if(queue == null){
			queue = new LinkedList<String>();
			queue.addAll(context.keySet());
		}

		while(!queue.isEmpty()){
			String s = queue.remove();
			AntlrExpressionParser p = new AntlrExpressionParser(context.get(s));
			ExpandSymbols expander = new ExpandSymbols(context);
			context.put(s, p.parseAndVisitWith(expander));
			expandSymbols(context, queue);
		}
		return context;
	}

}
