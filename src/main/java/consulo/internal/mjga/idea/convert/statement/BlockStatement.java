package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public class BlockStatement extends Statement
{
	private final List<GeneratedElement> myGeneratedElements;

	public BlockStatement(List<GeneratedElement> generatedElements)
	{
		myGeneratedElements = generatedElements;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		for(GeneratedElement generatedElement : myGeneratedElements)
		{
			builder.add(generatedElement.generate(true));
		}
		return builder.build();
	}
}
