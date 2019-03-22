/*
 * Copyright 2018, Hridesh Rajan, Ganesha Upadhyaya, Robert Dyer,
 *                 Bowling Green State University
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.graphs.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import boa.graphs.Node;
import boa.types.Ast.Expression;
import boa.types.Ast.Expression.ExpressionKind;
import boa.types.Ast.Statement;
import boa.types.Control.Node.NodeType;

/**
 * Control flow graph builder node
 *
 * @author ganeshau
 * @author rdyer
 */
public class CFGNode extends Node<CFGNode, CFGEdge> {
	private int methodId;
	private int objectNameId;
	private int classNameId;
	private int numOfParameters = 0;
	private HashSet<Integer> parameters;

	private static final HashMap<String, Integer> idOfLabel = new HashMap<String, Integer>();
	private static final HashMap<Integer, String> labelOfID = new HashMap<Integer, String>();

	private HashSet<String> useVariables;
	private String defVariables;

	public CFGNode() {
		super();
	}

	public CFGNode(final String methodName, final NodeType kind, final String className, final String objectName) {
		super(kind);
		this.methodId = convertLabel(methodName);
		this.classNameId = convertLabel(className);
		this.objectNameId = convertLabel(objectName);
	}

	public CFGNode(final String methodName, final NodeType kind, final String className,
			final String objectName, final int numOfParameters, final HashSet<Integer> datas) {
		super(kind);
		this.methodId = convertLabel(methodName);
		if (className == null) {
			this.classNameId = -1;
		} else {
			this.classNameId = convertLabel(className);
		}
		this.objectNameId = convertLabel(objectName);
		this.parameters = new HashSet<Integer>(datas);
		this.numOfParameters = numOfParameters;
	}

	public CFGNode(final String methodName, final NodeType kind, final String className,
			final String objectName, final int numOfParameters) {
		super(kind);
		this.methodId = convertLabel(methodName);
		this.classNameId = convertLabel(className);
		this.objectNameId = convertLabel(objectName);
		this.numOfParameters = numOfParameters;
	}

	public static int convertLabel(final String label) {
		if (!CFGNode.idOfLabel.containsKey(label)) {
			final int index = CFGNode.idOfLabel.size() + 1;
			CFGNode.idOfLabel.put(label, index);
			CFGNode.labelOfID.put(index, label);
			return index;
		}
		return CFGNode.idOfLabel.get(label);
	}

	public int getNumOfParameters() {
		return this.numOfParameters;
	}

	public void setParameters(final HashSet<Integer> parameters) {
		this.parameters = parameters;
	}

	public HashSet<Integer> getParameters() {
		return this.parameters;
	}

	public int getClassNameId() {
		return this.classNameId;
	}

	public int getObjectNameId() {
		return this.objectNameId;
	}

	public String getObjectName() {
		return labelOfID.get(this.objectNameId);
	}

	public String getClassName() {
		return labelOfID.get(this.classNameId);
	}

	public HashSet<String> getUseVariables() {
		if (this.useVariables == null)
			processUse();
		return this.useVariables;
	}

	public boolean hasDefVariables() {
		return this.defVariables != null;
	}

	public String getDefVariables() {
		if (this.defVariables == null)
			processDef();
		return this.defVariables;
	}

	public boolean hasFalseBranch() {
		for (final CFGEdge e : this.outEdges) {
			if (e.getLabel().equals("F"))
				return true;
		}
		return false;
	}

	public String getMethod() {
		return CFGNode.labelOfID.get(this.methodId);
	}

	public String getName() {
		String name = getMethod();
		if (name == null)
			name = getObjectName();
		if (name == null)
			name = getClassName();
		if (name == null)
			name = "";
		return name;
	}

	private void processDef() {
		String defVar = "";
		if (this.expr != null) {
			if (this.expr.getKind() == ExpressionKind.VARDECL) {
				final String[] strComponents = this.expr.getVariableDeclsList().get(0).getName().split("\\.");
				if (strComponents.length > 1) {
					defVar = strComponents[strComponents.length - 2];
				} else {
					defVar = strComponents[0];
				}
			} else if (this.expr.getKind() == ExpressionKind.OP_INC || this.expr.getKind() == ExpressionKind.OP_DEC) {
				if (this.expr.getExpressionsList().get(0).hasVariable()) {
					final String[] strComponents = this.expr.getExpressionsList().get(0).getVariable().split("\\.");
					if (strComponents.length > 1) {
						defVar = strComponents[strComponents.length - 2];
					} else {
						defVar = strComponents[0];
					}
				}
			} else if (this.expr.getKind().toString().startsWith("ASSIGN")) {
				final String[] strComponents = this.expr.getExpressionsList().get(0).getVariable().split("\\.");
				if (strComponents.length > 1) {
					defVar = strComponents[strComponents.length - 2];
				} else {
					defVar = strComponents[0];
				}
			}
		}
		this.defVariables = defVar;
	}

	private void processUse() {
		final HashSet<String> useVar = new HashSet<String>();
		if (this.expr != null) {
			if (this.expr.getKind() == ExpressionKind.ASSIGN) {
				processUse(useVar, this.expr.getExpressions(1));
			} else {
				processUse(useVar, this.expr);
			}
		}
		this.useVariables = useVar;
	}

	private static void processUse(final HashSet<String> useVar, final boa.types.Ast.Expression expr) {
		if (expr.hasVariable()) {
			if (expr.getExpressionsList().size() != 0) {
				useVar.add("this");
			} else {
				final String[] strComponents = expr.getVariable().split("\\.");
				if (strComponents.length > 1) {
					useVar.add(strComponents[strComponents.length - 2]);
				} else {
					useVar.add(strComponents[0]);
				}
			}
		}
		for (final boa.types.Ast.Expression exprs : expr.getExpressionsList()) {
			processUse(useVar, exprs);
		}
		for (final boa.types.Ast.Variable vardecls : expr.getVariableDeclsList()) {
			processUse(useVar, vardecls);
		}
		for (final boa.types.Ast.Expression methodexpr : expr.getMethodArgsList()) {
			processUse(useVar, methodexpr);
		}
	}

	private static void processUse(final HashSet<String> useVar, final boa.types.Ast.Variable vardecls) {
		if (vardecls.hasInitializer()) {
			processUse(useVar, vardecls.getInitializer());
		}
	}
}
