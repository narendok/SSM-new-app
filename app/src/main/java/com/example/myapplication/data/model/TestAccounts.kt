package com.example.myapplication.data.model

/** Existing synthetic identities; selecting one never grants authority. */
data class TestAccount(val label: String, val email: String, val password: String)
object TestAccounts {
    val all = DepartmentRole.entries.filter { !it.isLegacy }.map {
        TestAccount("${it.code} ${if (it.isClient || it == DepartmentRole.ADMIN) "" else "Lead"}", it.email, it.password)
    } + DepartmentRole.entries.filter { !it.isLegacy && !it.isClient && it != DepartmentRole.ADMIN }.map {
        val code = if (it == DepartmentRole.HROPS) "HR" else it.code
        TestAccount("$code Employee", "employee.${code.lowercase()}@example.com", "Demo@${code}EMP2026")
    }
}
