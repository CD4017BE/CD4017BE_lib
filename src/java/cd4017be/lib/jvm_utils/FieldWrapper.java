package cd4017be.lib.jvm_utils;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;


/**
 * @author CD4017BE
 *
 */
public class FieldWrapper<T> implements BooleanSupplier, IntSupplier, LongSupplier, DoubleSupplier, Supplier<T>, IntConsumer, LongConsumer, DoubleConsumer, Consumer<T> {

	private final Field field;
	public final Object owner;

	public FieldWrapper(Field field, Object owner) {
		this.field = field;
		this.owner = owner;
	}

	@Override
	public void accept(double arg0) {
		try {
			field.setDouble(owner, arg0);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void accept(long arg0) {
		try {
			field.setLong(owner, arg0);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void accept(int arg0) {
		try {
			field.setInt(owner, arg0);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void accept(T arg0) {
		try {
			field.set(owner, arg0);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getAsDouble() {
		try {
			return field.getDouble(owner);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getAsLong() {
		try {
			return field.getLong(owner);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getAsInt() {
		try {
			return field.getInt(owner);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean getAsBoolean() {
		try {
			return field.getBoolean(owner);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T) field.get(owner);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
