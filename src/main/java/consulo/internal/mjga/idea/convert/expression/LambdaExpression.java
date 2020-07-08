package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.statement.BlockStatement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class LambdaExpression extends Expression
{
	private List<CodeBlock> myParameters;

	private GeneratedElement myBlock;

	public LambdaExpression(List<CodeBlock> parameters, GeneratedElement block)
	{
		myParameters = parameters;
		myBlock = block;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();

		builder.beginControlFlow("($L) ->", CodeBlock.join(myParameters, ", "));
		if(myBlock instanceof BlockStatement)
		{
			for(GeneratedElement element : ((BlockStatement) myBlock).getGeneratedElements())
			{
				builder.addStatement(element.wantSemicolon(false).generate(false));
			}
		}
		else
		{
			builder.addStatement(myBlock.wantSemicolon(false).generate(false));
		}
		// this fix for 'endControlFlow()' we don't need new line
//		public Builder endControlFlow ()
//		{
//			unindent();
//			add("}\n");
//			return this;
//		}
		builder.unindent();
		builder.add("}");

		return builder.build();
	}
}
