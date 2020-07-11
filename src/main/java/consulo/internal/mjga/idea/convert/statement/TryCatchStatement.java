package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class TryCatchStatement extends Statement
{
	public static class Catch
	{
		public String name;
		public TypeName type;
		public GeneratedElement block;

		public Catch(String name, TypeName type, GeneratedElement block)
		{
			this.name = name;
			this.type = type;
			this.block = block;
		}
	}

	private final GeneratedElement myTryBlock;
	private final List<Catch> myCatches;

	public TryCatchStatement(GeneratedElement tryBlock, List<Catch> catches)
	{
		myTryBlock = tryBlock;
		myCatches = catches;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();

		builder.beginControlFlow("try");

		builder.add(myTryBlock.generate());

		builder.endControlFlow();

		for(Catch aCatch : myCatches)
		{
			builder.beginControlFlow("catch($T $L)", aCatch.type, aCatch.name);

			builder.add(aCatch.block.wantSemicolon(true).generate(true));

			builder.endControlFlow();
		}

		return builder.build();
	}
}
