package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-11
 */
public class ForEachStatement extends Statement
{
	private final TypeName myVarType;
	private final String myVarName;
	private final GeneratedElement myForElement;
	private final GeneratedElement myBlock;

	public ForEachStatement(TypeName varType, String varName, GeneratedElement forElement, GeneratedElement block)
	{
		myVarType = varType;
		myVarName = varName;
		myForElement = forElement;
		myBlock = block;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.beginControlFlow("for($T $L : $L)", myVarType, myVarName, myForElement.generate());
		builder.add(myBlock.generate());
		builder.endControlFlow();
		return builder.build();
	}
}
