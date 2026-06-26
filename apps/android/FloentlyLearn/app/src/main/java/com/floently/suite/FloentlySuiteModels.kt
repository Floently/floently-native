package com.floently.suite

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.design.FloentlyProduct

enum class FloentlySuiteProduct(
    val title: String,
    val subtitle: String,
    val accessProduct: FloentlyAccessProduct,
    val designProduct: FloentlyProduct
) {
    Learn(
        title = "Floently Learn",
        subtitle = "Finnish learning, YKI, roleplay, cards, professional Finnish, progress, and account.",
        accessProduct = FloentlyAccessProduct.Learn,
        designProduct = FloentlyProduct.Learn
    ),
    Read(
        title = "Floently Read",
        subtitle = "Upload, generate, and read with auto language detection and native reading controls.",
        accessProduct = FloentlyAccessProduct.Read,
        designProduct = FloentlyProduct.Read
    ),
    Create(
        title = "Floently Create Studio",
        subtitle = "Create-only studio for direct generation tools, saved projects, and exports.",
        accessProduct = FloentlyAccessProduct.Create,
        designProduct = FloentlyProduct.Create
    )
}

data class FloentlySuiteAccessState(
    val product: FloentlySuiteProduct,
    val isChecking: Boolean,
    val isAllowed: Boolean,
    val message: String?
)
