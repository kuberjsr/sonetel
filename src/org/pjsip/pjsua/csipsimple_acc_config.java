/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua;

public class csipsimple_acc_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  public csipsimple_acc_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public static long getCPtr(csipsimple_acc_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        pjsuaJNI.delete_csipsimple_acc_config(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setUse_zrtp(int value) {
    pjsuaJNI.csipsimple_acc_config_use_zrtp_set(swigCPtr, this, value);
  }

  public int getUse_zrtp() {
    return pjsuaJNI.csipsimple_acc_config_use_zrtp_get(swigCPtr, this);
  }

  public void setP_preferred_identity(pj_str_t value) {
    pjsuaJNI.csipsimple_acc_config_p_preferred_identity_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getP_preferred_identity() {
    long cPtr = pjsuaJNI.csipsimple_acc_config_p_preferred_identity_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public csipsimple_acc_config() {
    this(pjsuaJNI.new_csipsimple_acc_config(), true);
  }

}