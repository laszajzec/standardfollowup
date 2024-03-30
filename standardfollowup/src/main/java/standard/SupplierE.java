package standard;

@FunctionalInterface
public interface SupplierE<T> {
	public T get() throws Exception;
}
