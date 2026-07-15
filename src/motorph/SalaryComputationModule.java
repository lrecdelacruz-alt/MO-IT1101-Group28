package motorph;

// ============================================================
// SALARY COMPUTATION MODULE
// Handles all payroll computation logic for the MotorPH system.
//
// Responsibilities:
//   - Compute gross pay from hourly rate and hours worked
//   - Compute individual government deductions (SSS, PhilHealth,
//     Pag-IBIG, Withholding Tax)
//   - Compute total deductions
//   - Compute net pay
//
// All methods are static so they can be called without creating
// an instance. Each method handles one specific computation,
// keeping the logic modular and independently testable.
// ============================================================

public class SalaryComputationModule {

    // --------------------------------------------------------
    // GROSS PAY
    // --------------------------------------------------------

    /**
     * Calculates gross pay by multiplying the employee's hourly rate
     * by the total hours worked.
     *
     * @param hourlyRate  Employee's pay per hour
     * @param hoursWorked Total hours worked in the pay period
     * @return Computed gross pay amount
     */
    public static double computeGrossPay(double hourlyRate, double hoursWorked) {
        return hourlyRate * hoursWorked;
    }

    // --------------------------------------------------------
    // GOVERNMENT DEDUCTIONS
    // --------------------------------------------------------

    /**
     * Computes the SSS (Social Security System) contribution
     * based on the employee's gross pay using the official
     * SSS contribution table.
     *
     * Each bracket defines an upper salary limit and its
     * corresponding fixed contribution amount. The method
     * returns the contribution for the first matching bracket.
     * Salaries above the highest bracket use the maximum contribution.
     *
     * BUG FIX: the table's first bracket previously matched any gross
     * pay below 3250 — including exactly 0 — so an employee with no
     * recorded hours (e.g. missing attendance data) was still charged
     * a 135.00 contribution. Gross pay of 0 or less now correctly
     * returns 0.
     *
     * @param grossPay Employee's computed gross pay
     * @return SSS contribution amount
     */
    public static double computeSSS(double grossPay) {

        if (grossPay <= 0) {
            return 0;
        }

        // SSS contribution table: {upper salary limit, contribution amount}
        double[][] sssTable = {
            { 3250,  135.00}, { 3750,  157.50}, { 4250,  180.00}, { 4750,  202.50},
            { 5250,  225.00}, { 5750,  247.50}, { 6250,  270.00}, { 6750,  292.50},
            { 7250,  315.00}, { 7750,  337.50}, { 8250,  360.00}, { 8750,  382.50},
            { 9250,  405.00}, { 9750,  427.50}, {10250,  450.00}, {10750,  472.50},
            {11250,  495.00}, {11750,  517.50}, {12250,  540.00}, {12750,  562.50},
            {13250,  585.00}, {13750,  607.50}, {14250,  630.00}, {14750,  652.50},
            {15250,  675.00}, {15750,  697.50}, {16250,  720.00}, {16750,  742.50},
            {17250,  765.00}, {17750,  787.50}, {18250,  810.00}, {18750,  832.50},
            {19250,  855.00}, {19750,  877.50}, {20250,  900.00}, {20750,  922.50},
            {21250,  945.00}, {21750,  967.50}, {22250,  990.00}, {22750, 1012.50},
            {23250, 1035.00}, {23750, 1057.50}, {24250, 1080.00}, {24750, 1102.50}
        };

        for (double[] bracket : sssTable) {
            if (grossPay < bracket[0]) {
                return bracket[1];
            }
        }

        return 1125.00; // maximum contribution for salaries above highest bracket
    }

    /**
     * Computes the PhilHealth (Philippine Health Insurance) contribution
     * based on the employee's gross pay using the official rate table.
     *
     * Rules applied:
     *   - PHP 10,000 and below   : floored premium, 150 employee share
     *   - PHP 10,000 to 59,999  : 1.5% of gross pay
     *   - PHP 60,000 and above  : fixed cap of PHP 900
     *
     * @param grossPay Employee's computed gross pay
     * @return PhilHealth contribution amount
     */
    public static double computePhilHealth(double grossPay) {

        if (grossPay <= 0) {
            return 0;
        } else if (grossPay < 10000) {
            // Official MotorPH schedule floors the premium at the 10,000
            // salary level — fixed 300 total premium, 150 employee share —
            // rather than charging zero below that floor.
            return 150;
        } else if (grossPay < 60000) {
            return grossPay * 0.015; // 1.5% rate
        } else {
            return 900; // maximum cap
        }
    }

    /**
     * Computes the Pag-IBIG (HDMF) contribution based on the
     * employee's gross pay using the official rate table.
     *
     * Rules applied:
     *   - Below PHP 1,000    : no contribution
     *   - PHP 1,000–1,500   : 1% of gross pay
     *   - Above PHP 1,500   : 2% of gross pay
     *   - Maximum cap        : PHP 100
     *
     * @param grossPay Employee's computed gross pay
     * @return Pag-IBIG contribution amount (capped at PHP 100)
     */
    public static double computePagIBIG(double grossPay) {

        double contribution;

        if (grossPay < 1000) {
            contribution = 0;
        } else if (grossPay <= 1500) {
            contribution = grossPay * 0.01; // 1% rate
        } else {
            contribution = grossPay * 0.02; // 2% rate
        }

        return Math.min(contribution, 100.00); // enforce PHP 100 cap
    }

    /**
     * Computes the withholding tax based on the employee's taxable income
     * using the BIR tax bracket table.
     *
     * Taxable income = gross pay minus SSS, PhilHealth, and Pag-IBIG.
     * Each bracket defines an upper limit, excess base, tax rate, and
     * base tax amount. The method locates the correct bracket and computes
     * tax on the excess amount above the lower bound.
     *
     * @param taxableIncome Gross pay after mandatory deductions (SSS + PhilHealth + Pag-IBIG)
     * @return Computed withholding tax amount
     */
    public static double computeWithholdingTax(double taxableIncome) {

        if (taxableIncome <= 0) {
            return 0;
        }

        // BIR tax table: {upper limit, lower bound, rate, base tax}
        double[][] taxTable = {
            { 20832,      0, 0.00,    0.00},
            { 33333,  20833, 0.20,    0.00},
            { 66667,  33333, 0.25, 2500.00},
            {166667,  66667, 0.30, 10833.00},
            {666667, 166667, 0.32, 40833.33}
        };

        for (double[] bracket : taxTable) {
            if (taxableIncome <= bracket[0]) {
                double excess = taxableIncome - bracket[1];
                return bracket[3] + (excess * bracket[2]);
            }
        }

        // Highest bracket: above PHP 666,667
        return 200833.33 + (taxableIncome - 666667) * 0.35;
    }

    // --------------------------------------------------------
    // TOTAL DEDUCTIONS
    // --------------------------------------------------------

    /**
     * Computes total deductions by summing all government-mandated
     * contributions: SSS, PhilHealth, Pag-IBIG, and withholding tax.
     *
     * Withholding tax is computed on taxable income, which is gross pay
     * minus the other three deductions.
     *
     * BUG FIX: added an explicit zero-pay short-circuit. The individual
     * methods already guard against this on their own, but checking it
     * once here skips every bracket lookup for an employee with no
     * recorded hours instead of running all four anyway.
     *
     * @param grossPay Employee's computed gross pay
     * @return Total deductions amount
     */
    /**
     * Computes the full deduction breakdown (SSS, PhilHealth, Pag-IBIG, tax,
     * and total) in one pass. Both computeDeductions() and PayrollService's
     * generatePayslip() need these same four values — this way the bracket
     * lookups only run once instead of being duplicated in two places.
     *
     * @param grossPay Employee's computed gross pay
     * @return double[5]: {sss, philHealth, pagIbig, tax, totalDeductions}
     */
    public static double[] computeDeductionBreakdown(double grossPay) {
        if (grossPay <= 0) {
            return new double[]{0, 0, 0, 0, 0};
        }

        double sss        = computeSSS(grossPay);
        double philHealth = computePhilHealth(grossPay);
        double pagIbig     = computePagIBIG(grossPay);
        double taxable     = grossPay - (sss + philHealth + pagIbig);
        double tax         = computeWithholdingTax(taxable);
        double total       = sss + philHealth + pagIbig + tax;

        return new double[]{sss, philHealth, pagIbig, tax, total};
    }

    public static double computeDeductions(double grossPay) {
        return computeDeductionBreakdown(grossPay)[4];
    }

    // --------------------------------------------------------
    // NET PAY
    // --------------------------------------------------------

    /**
     * Computes the employee's net pay by subtracting total deductions
     * from gross pay.
     *
     * @param grossPay         Employee's computed gross pay
     * @param totalDeductions  Total government deductions
     * @return Net pay amount (take-home pay)
     */
    public static double computeNetPay(double grossPay, double totalDeductions) {
        return grossPay - totalDeductions;
    }
}