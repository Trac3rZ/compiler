package sizzle.types;

/**
 * A {@link ModifierKindProtoMap}.
 * 
 * @author rdyer
 */
public class ModifierKindProtoMap extends SizzleMap {
	/**
	 * Construct a {@link ModifierKindProtoMap}.
	 */
	public ModifierKindProtoMap() {
	}

	@Override
	public String toJavaType() {
		return "sizzle.types.Ast.Modifier.ModifierKind";
	}
}
