/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols;

import static net.sourceforge.pmd.util.CollectionUtil.listOf;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Fork/Override of upstream to fix some performance problems. See IMPROVED comments for details
 * <p>
 * Based on PMD 7.27.0
 */
@SuppressWarnings("all")
public interface SymbolResolver
{
	@Nullable
	JClassSymbol resolveClassFromBinaryName(@NonNull String binaryName);
	
	@Nullable
	JModuleSymbol resolveModule(@NonNull String moduleName);
	
	@Nullable
	JPackageSymbol resolvePackage(@NonNull String packageName);
	
	default @Nullable JClassSymbol resolveClassFromCanonicalName(@NonNull final String canonicalName)
	{
		// IMPROVED: DO NOT lookup possible outer classes
		// as it was never observed that this has been successfully the case
		// Currently this causes a lot of irrelevant class loading
		// e.g. lombok/extern/slf4j/Slf4j.class causes 4x irrelevant lookups when it's not at the classpath
		return this.resolveClassFromBinaryName(canonicalName);
	}
	
	static SymbolResolver layer(final SymbolResolver first, final SymbolResolver... others)
	{
		assert first != null : "Null first table";
		assert others != null : "Null array";
		assert !ArrayUtils.contains(others, null) : "Null component";
		
		return new SymbolResolver()
		{
			private final List<SymbolResolver> stack = listOf(first, others);
			
			@Override
			public @Nullable JClassSymbol resolveClassFromBinaryName(@NonNull final String binaryName)
			{
				for(final SymbolResolver resolver : this.stack)
				{
					final JClassSymbol sym = resolver.resolveClassFromBinaryName(binaryName);
					if(sym != null)
					{
						return sym;
					}
				}
				return null;
			}
			
			@Override
			public @Nullable JModuleSymbol resolveModule(@NonNull final String moduleName)
			{
				for(final SymbolResolver resolver : this.stack)
				{
					final JModuleSymbol symbol = resolver.resolveModule(moduleName);
					if(symbol != null)
					{
						return symbol;
					}
				}
				return null;
			}
			
			@Override
			public @Nullable JPackageSymbol resolvePackage(@NonNull final String packageName)
			{
				for(final SymbolResolver resolver : this.stack)
				{
					final JPackageSymbol symbol = resolver.resolvePackage(packageName);
					if(symbol != null)
					{
						return symbol;
					}
				}
				return null;
			}
			
			@Override
			public void logStats()
			{
				this.stack.forEach(SymbolResolver::logStats);
			}
		};
	}
	
	void logStats();
}
