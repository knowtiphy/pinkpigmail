package org.knowtiphy.pinkpigmail.calendarview

import com.calendarfx.model.Entry
import com.dlsc.formsfx.model.structure.Field
import com.dlsc.formsfx.model.structure.Form
import com.dlsc.formsfx.model.structure.Group
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import java.time.LocalDateTime
import java.util.*

/**
 * This class is used to create the form and holds all the necessary data. This
 * class acts as a singleton where the current instance is available using
 * `getInstance`.
 *
 * @author Sacha Schmid
 * @author Rinesch Murugathas
 */
class EntryModel(entry : Entry<*>)
{
	val title = SimpleStringProperty()
	val location = SimpleStringProperty()
	private val startDate = SimpleObjectProperty<LocalDateTime>()
	val startDateP = SimpleObjectProperty<LocalDateTime>()
	private val endDate = SimpleObjectProperty<LocalDateTime>()
	val endDateP = SimpleObjectProperty<LocalDateTime>()

	private var formInstance : Form? = null

	init
	{
		title.value = entry.title
		if (entry.location != null) location.value = entry.location
		startDate .value = entry.startAsLocalDateTime
		startDateP .value = entry.startAsLocalDateTime
		endDate.value = entry.endAsLocalDateTime
		endDateP .value = entry.endAsLocalDateTime
	}

	/**
	 * Creates or simply returns to form singleton instance.
	 *
	 * @return Returns the form instance.
	 */
	fun getFormInstance() : Form?
	{
		if (formInstance == null)
		{
			createForm()
		}
		return formInstance
	}

	/**
	 * Creates a new form instance with the required information.
	 */
	private fun createForm()
	{
		formInstance = Form.of(
			Group.of(
				//  why do I need the stupid spaces?
				Field.ofStringType(title).label("Title    ").placeholder("Title").editable(true),
				Field.ofStringType(location).label("Location").placeholder("Location").editable(true),
				DateTimeField(startDate, startDateP).label("Starts").editable(true),
				DateTimeField(endDate, endDateP).label("Ends").editable(true)
			)
//			Group.of(
//				Field.ofStringType(country.nameProperty()).label("country_label").placeholder("country_placeholder")
//					.validate(StringLengthValidator.atLeast(2, "country_error_message")),
//				Field.ofStringType(country.isoProperty()).label("ISO_3166_label").placeholder("ISO_3166_placeholder")
//					.validate(StringLengthValidator.exactly(2, "ISO_3166_error_message")),
//				Field.ofBooleanType(country.independenceProperty()).label("independent_label"),
//				Field.ofDate(country.getIndependenceDay()).label("independent_since_label")
//			), Section.of(
//				Field.ofStringType(country.currencyShortProperty()).label("currency_label")
//					.placeholder("currency_placeholder")
//					.validate(StringLengthValidator.exactly(3, "currency_error_message")).span(ColSpan.HALF),
//				Field.ofStringType(country.currencyLongProperty()).label("currency_long_label")
//					.placeholder("currency_long_placeholder").span(ColSpan.HALF),
//				Field.ofDoubleType(country.areaProperty()).label("area_label").format("format_error_message")
//					.placeholder("area_placeholder").validate(DoubleRangeValidator.atLeast(1.0, "area_error_message"))
//					.span(ColSpan.HALF),
//				Field.ofStringType(country.tldProperty()).label("internet_TLD_label")
//					.placeholder("internet_TLD_placeholder").span(ColSpan.HALF).validate(
//						StringLengthValidator.exactly(3, "internet_TLD_error_message"),
//						RegexValidator.forPattern("^.[a-z]{2}$", "internet_TLD_format_error_message")
//					),
//				Field.ofStringType(country.dateFormatProperty()).label("date_format_label")
//					.placeholder("date_format_placeholder").multiline(true).span(ColSpan.HALF)
//					.validate(StringLengthValidator.atLeast(8, "date_format_error_message")),
//				Field.ofSingleSelectionType(country.allSidesProperty(), country.driverSideProperty())
//					.required("required_error_message").label("driving_label").span(ColSpan.HALF)
//					.render(SimpleRadioButtonControl()),
//				Field.ofStringType(country.timeZoneProperty()).label("time_zone_label")
//					.placeholder("time_zone_placeholder").span(ColSpan.HALF)
//					.validate(StringLengthValidator.exactly(3, "time_zone_error_message")),
//				Field.ofStringType(country.summerTimeZoneProperty()).label("summer_time_zone_label")
//					.placeholder("summer_time_zone_placeholder").span(ColSpan.HALF)
//					.validate(StringLengthValidator.atLeast(3, "summer_time_zone_error_message"))
//			).title("other_information_label"), Section.of(
//				Field.ofSingleSelectionType(country.allCapitalsProperty(), country.capitalProperty())
//					.label("capital_label").required("required_error_message").tooltip("capital_tooltip")
//					.span(ColSpan.HALF),
//				Field.ofIntegerType(country.populationProperty()).label("population_label")
//					.format("format_error_message").placeholder("population_placeholder")
//					.validate(IntegerRangeValidator.atLeast(1, "population_error_message")),
//				Field.ofMultiSelectionType(country.allContinentsProperty(), country.continentsProperty())
//					.label("continent_label").required("required_error_message").span(ColSpan.HALF)
//					.render(SimpleCheckBoxControl()),
//				Field.ofMultiSelectionType(country.allCitiesProperty(), country.germanCitiesProperty())
//					.label("german_cities_label").span(ColSpan.HALF),
//				Field.ofPasswordType("secret").label("secret_label").required("required_error_message")
//					.span(ColSpan.HALF).validate(StringLengthValidator.between(1, 10, "secret_error_message"))
//			).title("cities_and_population_label")
		).title("Event Details")//.i18n(rbs)
	}

	/**
	 * Sets the locale of the form.
	 *
	 * @param language The language identifier for the new locale. Either DE or EN.
	 */
//	fun translate(language : String?)
//	{
//		when (language)
//		{
//			"EN" -> rbs.changeLocale(rbEN)
//			"DE" -> rbs.changeLocale(rbDE)
//			else -> throw IllegalArgumentException("Not a valid locale")
//		}
//	}

//	fun getCountry() : Country
//	{
//		return country
//	}
}