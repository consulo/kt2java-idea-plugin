package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class LocalVariableStatement extends Statement
{
	private TypeName myType;
	private String myName;
	private GeneratedElement myInitializer;

	public LocalVariableStatement(TypeName type, String name, GeneratedElement initializer)
	{
		myType = type;
		myName = name;
		myInitializer = initializer;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		if(myInitializer != null)
		{
			return CodeBlock.of(wrap("$T $L = $L", needNewLine), myType, myName, myInitializer.generate());
		}
		return CodeBlock.of(wrap("$T $L", needNewLine), myType, myName);
	}

	@Override
	protected boolean isAllowSemicolon()
	{
		return true;
	}
}
