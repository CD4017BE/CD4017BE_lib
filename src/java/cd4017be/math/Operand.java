package cd4017be.math;

/** Represents a general Mathematical operand to support polymorphic algorithms.
 * @param <T> the actual operand type
 * @author cd4017be */
@SuppressWarnings("unchecked")
public interface Operand<T extends Operand<T>> {

	/** @return this = o */
	T set(T o);

	/** @return this = a + b */
	T sum(T a, T b);

	/** @return this += o */
	default T add(T o) {
		return sum((T)this, o);
	}

	/** @return this = a - b */
	T dif(T a, T b);

	/** @return this -= o */
	default T sub(T o) {
		return dif((T)this, o);
	}

	/** @return this = o - this */
	default T rsub(T o) {
		return dif(o, (T)this);
	}

	/** @return this = a * b */
	T prod(T a, T b);

	/** @return this *= o */
	default T mul(T o) {
		return prod((T)this, o);
	}

	/** @return this = o * this */
	default T rmul(T o) {
		return prod(o, (T)this);
	}

	/** @return this = a / b */
	T quot(T a, T b);

	/** @return this /= o */
	default T div(T o) {
		return quot((T)this, o);
	}

	/** @return this = o / this */
	default T rdiv(T o) {
		return quot(o, (T)this);
	}

	/** @return this = a % b */
	T rem(T a, T b);

	/** @return this %= o */
	default T mod(T o) {
		return rem((T)this, o);
	}

	/** @return this = o % this */
	default T rmod(T o) {
		return rem(o, (T)this);
	}

	/** @return this = -o */
	T neg(T o);

	/** @return this = -this */
	default T neg() {
		return neg((T)this);
	}

	/** @return this = 1/o */
	T rec(T o);

	/** @return this = 1/this */
	default T rec() {
		return rec((T)this);
	}

	/** @return this == o */
	boolean equals(T o);

	T clone();

	/** An operand operating with integers
	 * @param <T> the actual operand type
	 * @author cd4017be */
	public interface I<T extends I<T>>extends Operand<T> {

		/** @return this = o * s */
		T sca(T o, int s);

		/** @return this *= s */
		default T sca(int s) {
			return sca((T)this, s);
		}

		/** @return this = o / s */
		T sinv(T o, int s);

		/** @return this /= s */
		default T sinv(int s) {
			return sinv((T)this, s);
		}

		/** @return this = s / o */
		T srec(T o, int s);

		/** @return this = s / this */
		default T srec(int s) {
			return srec((T)this, s);
		}

		/** @return |this|² */
		int asqI();
	}


	/** An operand operating with floats
	 * @param <T> the actual operand type
	 * @author cd4017be */
	public interface F<T extends F<T>>extends Operand<T> {

		/** @return this = o * s */
		T sca(T o, float s);

		/** @return this *= s */
		default T sca(float s) {
			return sca((T)this, s);
		}

		/** @return this = s / o */
		default T srec(T o, float s) {
			return sca(o, s / o.asqF());
		}

		/** @return this = s / this */
		default T srec(float s) {
			return srec((T)this, s);
		}

		default T rec(T o) {
			return srec(o, 1F);
		}

		default T neg(T o) {
			return sca(o, -1F);
		}

		/** @return |this|² */
		float asqF();

	}


	/** An operand operating with doubles
	 * @param <T> the actual operand type
	 * @author cd4017be */
	public interface D<T extends D<T>>extends Operand<T> {

		/** @return this = o * s */
		T sca(T o, double s);
		
		/** @return this *= s */
		default T sca(double s) {
			return sca((T)this, s);
		}

		/** @return this = s / this */
		default T srec(double s) {
			return sca(s / asqD());
		}

		/** @return this = s / o */
		default T srec(T o, double s) {
			return sca(o, s / o.asqD());
		}

		default T rec(T o) {
			return srec(o, 1D);
		}

		default T neg(T o) {
			return sca(o, -1D);
		}

		/** @return |this|² */
		double asqD();

		/** @return |this| */
		default double abs() {
			return Math.sqrt(asqD());
		}
	}

}
