package motorph;

// ============================================================
// DATA CHANGE LISTENER (Observer pattern)
//
// Implemented by any GUI window that needs to stay in sync with
// PayrollService's employee data. PayrollService notifies every
// registered listener whenever employee data actually changes —
// whether that change came from this window or a different one.
//
// Why this exists (bug fix):
//   EmployeeMenu and PayrollMenu are separate windows that share
//   one PayrollService instance. Without this, running "Compute
//   Salaries" in PayrollMenu left an already-open EmployeeMenu
//   showing stale Hours/Gross/Net values until the user manually
//   triggered some other refresh.
//
// ============================================================
public interface DataChangeListener {
    void onDataChanged();
}