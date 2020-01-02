package org.knowtiphy.pinkpigmail.util

class MatchFunction<S, T, U>(private val f : (S, T) -> U) : java.util.function.Function<S, U?>
{
	private val map = HashMap<S, U>()

	fun addMatch(s: S, t: T)
	{
		map[s] = f.invoke(s, t)
	}

	override fun apply(s: S) : U?
	{
		return map.remove(s)
	}
}