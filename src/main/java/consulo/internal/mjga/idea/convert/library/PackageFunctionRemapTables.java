package consulo.internal.mjga.idea.convert.library;

import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.HashMap;
import java.util.Map;

public class PackageFunctionRemapTables
{
	public static final Map<Map.Entry<FqName, Name>, PackageFunctionRemapTable> table;

	static
	{
		table = new HashMap<>();
		// init
		PackageFunctionRemapTable.values();
	}
}
