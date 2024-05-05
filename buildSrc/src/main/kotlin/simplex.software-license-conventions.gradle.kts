import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ExcludeDependenciesWithoutArtifactsFilter

plugins {
    id("com.github.jk1.dependency-license-report")
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(
        InventoryHtmlReportRenderer("report.html","Backend"),
        JsonReportRenderer("report.json", true),
    )
    filters = arrayOf<DependencyFilter>(
        LicenseBundleNormalizer(),
        ExcludeDependenciesWithoutArtifactsFilter(),
    )
}
