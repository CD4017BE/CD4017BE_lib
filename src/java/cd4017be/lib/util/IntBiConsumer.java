package cd4017be.lib.util;


/**
 * Represents an operation that accepts two int arguments and returns no result. This is a primitive type specialization of BiConsumer.<dl>
 * This is a functional interface whose functional method is accept(int, int).
 * @author CD4017BE
 * @see java.util.function.IntConsumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface IntBiConsumer {

	void accept(int a, int b);

}
