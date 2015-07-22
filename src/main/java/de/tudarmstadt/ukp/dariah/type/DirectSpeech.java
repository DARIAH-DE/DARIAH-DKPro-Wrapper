

/* First created by JCasGen Thu Jan 08 15:18:26 CET 2015 */
package de.tudarmstadt.ukp.dariah.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Fri Jan 09 13:48:49 CET 2015
 * XML source: /home/local/UKP/dhyani/Desktop/Pipeline/src/main/java/de/tudarmstadt/ukp/dariah/types/DirectSpeechSystemDescriptor.xml
 * @generated */
public class DirectSpeech extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DirectSpeech.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected DirectSpeech() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public DirectSpeech(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public DirectSpeech(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public DirectSpeech(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
}

    