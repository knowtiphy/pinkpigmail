package org.knowtiphy.pinkpigmail.util

import java.util.*
import java.util.concurrent.Callable

object Functions
{
	fun <S, T : Comparable<T>> cmp(e: (S) -> T?): Comparator<S>
	{
		return kotlin.Comparator { x, y ->
			val a = e.invoke(x)
			val b = e.invoke(y)
			val result = if (a == null) if (b == null) 0 else 1 else if (b == null) -1 else a.compareTo(b)
			result
		}
	}

	fun <T> callable(f: () -> T): Callable<T>
	{
		return Callable { f.invoke() }
	}
}