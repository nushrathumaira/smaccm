/**
 */
package edu.uah.rsesc.aadlsimulator.xtext.inputConstraint;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Random Element Expression</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link edu.uah.rsesc.aadlsimulator.xtext.inputConstraint.RandomElementExpression#getSet <em>Set</em>}</li>
 * </ul>
 *
 * @see edu.uah.rsesc.aadlsimulator.xtext.inputConstraint.InputConstraintPackage#getRandomElementExpression()
 * @model
 * @generated
 */
public interface RandomElementExpression extends RandomExpression
{
  /**
   * Returns the value of the '<em><b>Set</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Set</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Set</em>' containment reference.
   * @see #setSet(SetExpression)
   * @see edu.uah.rsesc.aadlsimulator.xtext.inputConstraint.InputConstraintPackage#getRandomElementExpression_Set()
   * @model containment="true"
   * @generated
   */
  SetExpression getSet();

  /**
   * Sets the value of the '{@link edu.uah.rsesc.aadlsimulator.xtext.inputConstraint.RandomElementExpression#getSet <em>Set</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Set</em>' containment reference.
   * @see #getSet()
   * @generated
   */
  void setSet(SetExpression value);

} // RandomElementExpression