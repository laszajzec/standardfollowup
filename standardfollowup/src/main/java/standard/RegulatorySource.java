package standard;

public interface RegulatorySource {
	
	void collect() throws Exception;
	void removeUnchanged() throws Exception;
	void evaluate() throws Exception;
	boolean isOk();
}
