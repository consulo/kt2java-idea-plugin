package consulo.internal.mjga.idea.convert.type;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.KotlinType;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public interface TypeRemapper
{
	@Nullable
	TypeName remap(@NotNull KotlinType kotlinType);
}
