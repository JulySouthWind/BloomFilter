package my.based.learn.algorithm;

/*
 * Implementation of a Bloom-filter 
 * 
 * @param <E>
 * 		Object type is to be insert into the bloom filter,e.g.
 * 		String or Integer or Long
 * 
 * @author : qingwu.fu <qingwufu@gmail.com>
 * @date : 2013-06-19
 * 
 */
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;

public class BloomFilter<E> implements Serializable {

	private int bitSetSize;
	
	private int exepectElementNum; 
	
	private int functions;
	
	private double bitPerEle;
	
	private int addEleNum;
	
	public BitSet bitBuf; 
	
	static final String hashName = "MD5"; // MD5 gives good enough accuracey in most circumstances. Change to SHA1 if it's needed
	static final Charset charset = Charset.forName("UTF-8"); // encoding used for storing hash valuses as strings
	
	static final MessageDigest digestFunction;
	
	static {
		MessageDigest tmp;
		try{
			tmp = MessageDigest.getInstance(hashName);
		} catch (NoSuchAlgorithmException e){
			tmp = null;
		}
		digestFunction = tmp;
	}
	
	/*
	 * The total length of the Bloom Filter will be bitPerEle * elementNum
	 * 
	 *  @param bitPerEle 
	 *  			the number of bits used per element
	 *  @param elementNum
	 *  			the expected number of elements the filter will contain
	 *  @param funcNum
	 *  			the number of hash functions used
	 */
	
	public BloomFilter(double bitPerEle, int elementNum, int funcNum){
		this.bitPerEle = bitPerEle;
		this.exepectElementNum = elementNum;
		this.functions = funcNum;
		this.addEleNum = 0;
		this.bitSetSize = (int)Math.ceil(bitPerEle * elementNum); 
		this.bitBuf = new BitSet(this.bitSetSize);
	}
	
	/*
	 * @param bitSetSize
	 * 			defines how many bits should be used in total for the filter
	 * @param expectElementNum
	 * 			defines the maximum number of elements the filter is expected to contain
	 * 
	 * The optimal number of hash functions is estimated from the total size of the Bloom and the number of expected elements.
	 *  
	 */
	public BloomFilter(int bitSetSize, int expectElementNum){
		this((double)bitSetSize/expectElementNum, expectElementNum, (int) Math.round((bitSetSize/(double)expectElementNum)*Math.log(2.0)));
	}
	
	/*
	 * Generates a digest based on the contents of a String
	 * 
	 * @param val
	 * 		specifies the input data
	 * 
	 * @param charset
	 * 		specifies the encoding of the input data
	 * 		
	 */
	public static long createHash(String val, Charset charset){
		return createHash(val.getBytes(charset));
		
	}
	
	/*
	 * @param val
	 * 		specifies the input data.The encoding is expected to be UTF-8
	 * 
	 */
	public static long createHash(String val){
		return createHash(val, charset);
	}
	
	/*
	 * @param data
	 * 		specifies input data
	 * @return digest as long
	 */
	public static long createHash(byte[] data){
		long h = 0;
		byte[] res;
		
		synchronized (digestFunction){
			res = digestFunction.digest(data);
		}
		
		//将四个字节的 byte 类型转换成 long 型
		for (int i = 0; i < 4; i++){
			h <<= 8;
			h |= ((int) res[i]) & 0xFF;
		}
		return h;
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		final BloomFilter<E> other = (BloomFilter) obj;
		
		if (this.bitSetSize != other.bitSetSize)
			return false;
		
		if (this.exepectElementNum != other.exepectElementNum)
			return false;
		
		if (this.functions != other.functions)
			return false;
		
		if (this.bitBuf != other.bitBuf 
				&& (this.bitBuf == null || !this.bitBuf.equals(other.bitBuf)))
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode(){
		int hash = 7;
		
		hash = 31 * hash + (this.bitBuf != null ? this.bitBuf.hashCode() : 0 );
		hash = 31 * hash + this.exepectElementNum;
		hash = 31 * hash + this.bitSetSize;
		hash = 31 * hash + this.functions;
		
		return hash;
	}
	
	/*
	 * @return expected probability of false positive
	 */
	public double expectedFalsePositiveProbability(){
		return getFalsePositiveProbability(this.exepectElementNum);
	}
	
	/*
	 * @param elementNum
	 * 		number of inserted elements
	 * @return probability of a false positive.
	 * 
	 */
	public double getFalsePositiveProbability(double elementNum){
		return Math.pow((1 - Math.exp(-this.functions * (double) elementNum/(double)this.bitSetSize)), this.functions);
	}
	
	/*
	 * Sets all bits to false in the Bloom filter
	 */
	public void clear(){
		this.bitBuf.clear();
		this.addEleNum = 0;
	}
	
	/*
	 * Adds an object to the Bloom filter. The output from the object's toString() method is used as input to the hash functions.
	 * 
	 * @param element
	 */
	public void add(E element){
		long hash;
		String valString = element.toString();
		
		for (int x = 0; x < this.functions; x++){
			hash = createHash(valString + Integer.toString(x));
			hash = hash % (long) this.bitSetSize;
			bitBuf.set(Math.abs((int)hash), true);
		}
		this.addEleNum++;
	}
	
	/*
	 * Adds all elements from a Collection to the Bloom filter.
	 * 
	 * @param c
	 * 		Collection of elements.
	 */
	public void addAll(Collection<? extends E> c){
		for (E element : c){
			add(element);
		}
	}
	
	/*
	 * @param element
	 * 		element to check
	 * @return true or false
	 */
	public boolean contains(E element){
		long hash;
		String valString = element.toString();
		
		for (int x = 0; x < this.functions; x++){
			hash = createHash(valString + Integer.toString(x));
			hash = hash % (long) this.bitSetSize;
			if (!this.bitBuf.get(Math.abs((int) hash)))
					return false;
		}
		return true;
	}
	
	/*
	 * @param c
	 * 			elements to check
	 * @return true or false
	 */
	public boolean containsAll(Collection<? extends E> c){
		for (E element : c)
			if (!contains(element))
				return false;
		return true;
	}
	
	/*
	 *  Get and set a single bit
	 */
	public boolean getBit(int index){
		return this.bitBuf.get(index);
	}
	
	public void setBit(int index, boolean value){
		this.bitBuf.set(index, value);
	}
	
	/*
	 * Return the bit set used to store the Bloom filter
	 * 
	 * @return bit set
	 */
	public BitSet getBitSet() {
		return this.bitBuf;
	}
	
	/*
	 * Return the number of bits in the Bloom filter.
	 * 
	 * @return the size of the bitset usef by the Bloom filter
	 */
	public int size() {
		return this.bitSetSize;
	}
	
	/*
	 * Return the number of elements added to the Bloom filter after it was constructed after clear() was called
	 * 
	 * @return number of elements added to the Bloom filter.
	 */
	public int count(){
		return this.addEleNum;
	}
	
	
	public int getExpectedNumberOfElements(){
		return this.exepectElementNum;
	}
	
	public double getExpectedBitsPerElement(){
		return this.bitPerEle;
	}
	
	public double getBitsPerElement() {
		return this.bitSetSize / (double) this.addEleNum;
	}
	
	
	
	
	
	public static void main(String[] args){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = new Date();
		Date d1 = new Date(d.getTime() - 24*60*60*1000);
		System.out.println(sdf.format(d) + " \t" + sdf.format(d1));
		
		
	}
	
	
}
