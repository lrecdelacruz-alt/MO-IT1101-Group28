package motorph;

// ============================================================
// DATA CHANGE LISTENER
//
// Defines the contract for components that need to respond
// when employee data changes.
//
// Implementing classes refresh their displayed information
// after employee records or payroll data are updated.
//
// ============================================================
public interface DataChangeListener {

    /**
    * Responds to changes in employee or payroll data.
    *
    * Implementing classes update their displayed
    * information when this method is invoked.
    */
    void onDataChanged();
}