package consulo.internal.mjga.idea.convert.expression;

import com.intellij.openapi.util.Pair;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.statement.BlockStatement;
import consulo.internal.mjga.idea.convert.statement.Statement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class LambdaExpression extends Expression
{
	private List<Pair<TypeName, String>> myParameters;

	private GeneratedElement myBlock;

	public LambdaExpression(List<Pair<TypeName, String>> parameters, GeneratedElement block)
	{
		myParameters = parameters;
		myBlock = block;
	}

	public GeneratedElement getBlock()
	{
		return myBlock;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();

		StringBuilder paramsBuilder = new StringBuilder();
		if(myParameters.size() == 1)
		{
			Pair<TypeName, String> pair = myParameters.get(0);

			if(pair.getFirst() == null)
			{
				paramsBuilder.append(CodeBlock.of("$L", pair.getSecond()));
			}
			else
			{
				paramsBuilder.append(CodeBlock.of("$T $L", pair.getFirst(), pair.getSecond()));
			}
			paramsBuilder.append(" -> ");
		}
		else
		{
			paramsBuilder.append("(");
			for(int p = 0; p < myParameters.size(); p++)
			{
				if(p != 0)
				{
					paramsBuilder.append(", ");
				}

				Pair<TypeName, String> pair = myParameters.get(p);

				if(pair.getFirst() == null)
				{
					paramsBuilder.append(CodeBlock.of("$L", pair.getSecond()));
				}
				else
				{
					paramsBuilder.append(CodeBlock.of("$T $L", pair.getFirst(), pair.getSecond()));
				}
			}
			paramsBuilder.append(") ->");
		}

		if(myBlock instanceof Statement statement)
		{
			builder.beginControlFlow(paramsBuilder.toString());
			if(myBlock instanceof BlockStatement blockStatement)
			{
				for(GeneratedElement element : ((BlockStatement) myBlock).getGeneratedElements())
				{
					builder.addStatement(element.wantSemicolon(false).generate(false));
				}
			}
			else
			{
				builder.addStatement(statement.wantSemicolon(false).generate(false));
			}

			// this fix for 'endControlFlow()' we don't need new line
			//		public Builder endControlFlow ()
			//		{
			//			unindent();
			//			add("}\n");
			//			return this;
			//		}
			//builder.endControlFlow();
			builder.unindent();
			builder.add("}");
		}
		else
		{
			builder.add(paramsBuilder.toString());
			builder.add(myBlock.wantSemicolon(false).generate(false));
		}

		return builder.build();
	}
}
