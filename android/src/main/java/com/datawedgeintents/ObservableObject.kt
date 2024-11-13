package com.datawedgeintents

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

//import java.util.Observable
//
//class ObservableObject private constructor() : Observable() {
//  fun updateValue(data: Any?) {
//    synchronized(this) {
//      setChanged()
//      notifyObservers(data)
//    }
//  }
//
//  companion object {
//    val instance: ObservableObject = ObservableObject()
//  }
//}


class ObservableObject {
  private var property: String? = null
  private val support = PropertyChangeSupport(this)

  fun addPropertyChangeListener(listener: PropertyChangeListener?) {
    support.addPropertyChangeListener(listener)
  }

  fun removePropertyChangeListener(listener: PropertyChangeListener?) {
    support.removePropertyChangeListener(listener)
  }

  fun setProperty(value: String?) {
    val oldValue = this.property
    this.property = value
    support.firePropertyChange("property", oldValue, value)
  }
}
