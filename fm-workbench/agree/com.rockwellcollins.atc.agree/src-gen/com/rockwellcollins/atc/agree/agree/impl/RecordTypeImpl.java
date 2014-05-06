/**
 */
package com.rockwellcollins.atc.agree.agree.impl;

import com.rockwellcollins.atc.agree.agree.AgreePackage;
import com.rockwellcollins.atc.agree.agree.NestedDotID;
import com.rockwellcollins.atc.agree.agree.RecordType;
import com.rockwellcollins.atc.agree.agree.RecordTypeDefExpr;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Record Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.rockwellcollins.atc.agree.agree.impl.RecordTypeImpl#getFeatureGroup <em>Feature Group</em>}</li>
 *   <li>{@link com.rockwellcollins.atc.agree.agree.impl.RecordTypeImpl#getRecord <em>Record</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RecordTypeImpl extends TypeImpl implements RecordType
{
  /**
   * The cached value of the '{@link #getFeatureGroup() <em>Feature Group</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getFeatureGroup()
   * @generated
   * @ordered
   */
  protected NestedDotID featureGroup;

  /**
   * The cached value of the '{@link #getRecord() <em>Record</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getRecord()
   * @generated
   * @ordered
   */
  protected RecordTypeDefExpr record;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected RecordTypeImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return AgreePackage.Literals.RECORD_TYPE;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NestedDotID getFeatureGroup()
  {
    return featureGroup;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetFeatureGroup(NestedDotID newFeatureGroup, NotificationChain msgs)
  {
    NestedDotID oldFeatureGroup = featureGroup;
    featureGroup = newFeatureGroup;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, AgreePackage.RECORD_TYPE__FEATURE_GROUP, oldFeatureGroup, newFeatureGroup);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setFeatureGroup(NestedDotID newFeatureGroup)
  {
    if (newFeatureGroup != featureGroup)
    {
      NotificationChain msgs = null;
      if (featureGroup != null)
        msgs = ((InternalEObject)featureGroup).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - AgreePackage.RECORD_TYPE__FEATURE_GROUP, null, msgs);
      if (newFeatureGroup != null)
        msgs = ((InternalEObject)newFeatureGroup).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - AgreePackage.RECORD_TYPE__FEATURE_GROUP, null, msgs);
      msgs = basicSetFeatureGroup(newFeatureGroup, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, AgreePackage.RECORD_TYPE__FEATURE_GROUP, newFeatureGroup, newFeatureGroup));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public RecordTypeDefExpr getRecord()
  {
    if (record != null && record.eIsProxy())
    {
      InternalEObject oldRecord = (InternalEObject)record;
      record = (RecordTypeDefExpr)eResolveProxy(oldRecord);
      if (record != oldRecord)
      {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, AgreePackage.RECORD_TYPE__RECORD, oldRecord, record));
      }
    }
    return record;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public RecordTypeDefExpr basicGetRecord()
  {
    return record;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setRecord(RecordTypeDefExpr newRecord)
  {
    RecordTypeDefExpr oldRecord = record;
    record = newRecord;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, AgreePackage.RECORD_TYPE__RECORD, oldRecord, record));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
      case AgreePackage.RECORD_TYPE__FEATURE_GROUP:
        return basicSetFeatureGroup(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
      case AgreePackage.RECORD_TYPE__FEATURE_GROUP:
        return getFeatureGroup();
      case AgreePackage.RECORD_TYPE__RECORD:
        if (resolve) return getRecord();
        return basicGetRecord();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case AgreePackage.RECORD_TYPE__FEATURE_GROUP:
        setFeatureGroup((NestedDotID)newValue);
        return;
      case AgreePackage.RECORD_TYPE__RECORD:
        setRecord((RecordTypeDefExpr)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
      case AgreePackage.RECORD_TYPE__FEATURE_GROUP:
        setFeatureGroup((NestedDotID)null);
        return;
      case AgreePackage.RECORD_TYPE__RECORD:
        setRecord((RecordTypeDefExpr)null);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
      case AgreePackage.RECORD_TYPE__FEATURE_GROUP:
        return featureGroup != null;
      case AgreePackage.RECORD_TYPE__RECORD:
        return record != null;
    }
    return super.eIsSet(featureID);
  }

} //RecordTypeImpl