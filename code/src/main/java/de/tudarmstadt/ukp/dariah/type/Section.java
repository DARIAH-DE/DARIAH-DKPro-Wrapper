

/* First created by JCasGen Thu Oct 22 12:42:54 CEST 2015 */
package de.tudarmstadt.ukp.dariah.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Oct 22 12:42:54 CEST 2015
 * XML source: /home/likewise-open/UKP/reimers/Dropbox/Doktor/DARIAH/git/code/src/main/resources/desc/type/SectionSystemDescriptor.xml
 * @generated */
public class Section extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Section.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Section() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Section(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Section(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Section(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: value

  /** getter for value - gets 
   * @generated
   * @return value of the feature 
   */
  public String getValue() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.tudarmstadt.ukp.dariah.type.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_value);}
    
  /** setter for value - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setValue(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.tudarmstadt.ukp.dariah.type.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_value, v);}    
  }

    