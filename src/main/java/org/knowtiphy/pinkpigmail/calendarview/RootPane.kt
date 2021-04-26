package org.knowtiphy.pinkpigmail.calendarview

import com.dlsc.formsfx.view.renderer.FormRenderer
import com.dlsc.formsfx.view.util.ViewMixin
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane

class RootPane(private val model : EntryModel) : BorderPane(), ViewMixin
{
	/**
	 * - controls holds all the nodes which are rendered on the right side.
	 * - scrollContent holds the form.
	 * - languageButtons holds both buttons to change language.
	 * - statusContent holds all the state change labels.
	 * - formButtons holds the save and reset button.
	 * - toggleContent holds the buttons to toggle editable and sections.
	 * - bindingInfo holds the info of property changes.
	 */
//	private var controls : GridPane? = null
	private var scrollContent : ScrollPane? = null
//	private var statusContent : VBox? = null
//	private var formButtons : HBox? = null
//	private var toggleContent : VBox? = null
//	private var bindingInfo : VBox? = null
//	private var save : Button? = null
//	private var reset : Button? = null
//	private var languageDE : Button? = null
//	private var languageEN : Button? = null
//	private var validLabel : Label? = null
//	private var changedLabel : Label? = null
//	private var persistableLabel : Label? = null
//	private var countryLabel : Label? = null
//	private var currencyLabel : Label? = null
//	private var populationLabel : Label? = null
//	private var editableToggle : Button? = null
//	private var sectionToggle : Button? = null
	private var displayForm : FormRenderer? = null

	/**
	 * This method is used to set up the stylesheets.
	 */
	override fun initializeSelf()
	{
//		stylesheets.add(javaClass.getResource("/style.css").toExternalForm())
//		styleClass.add("root-pane")
	}

	/**
	 * This method initializes all nodes and regions.
	 */
	override fun initializeParts()
	{
//		save = Button("Save")
//		reset = Button("Reset")
//		save!!.styleClass.add("save-button")
//		reset!!.styleClass.add("reset-button")

		// The language buttons get a picture of the country from the flaticon
		// font in the css.
//		languageDE = Button("\ue001")
//		languageEN = Button("\ue000")
//		validLabel = Label("The form is valid.")
//		persistableLabel = Label("The form is not persistable.")
//		changedLabel = Label("The form has not changed.")
//		countryLabel = Label("Country: " + model.getCountry().getName())
//		currencyLabel = Label("Currency: " + model.getCountry().getCurrencyShort())
//		populationLabel = Label("Population: " + model.getCountry().getPopulation())
//		editableToggle = Button("Toggle Editable")
//		sectionToggle = Button("Toggle Sections")
//		editableToggle!!.styleClass.add("toggle-button")
//		sectionToggle!!.styleClass.add("toggle-button")
	//	statusContent = VBox()
//		formButtons = HBox()
//		toggleContent = VBox()
//		bindingInfo = VBox()
	//	controls = GridPane()
		scrollContent = ScrollPane()
		displayForm = FormRenderer(model.getFormInstance())
	}

	/**
	 * This method sets up the necessary bindings for the logic of the
	 * application.
	 */
	override fun setupBindings()
	{
		//save!!.disableProperty().bind(model.getFormInstance()!!.persistableProperty().not())
		//reset!!.disableProperty().bind(model.getFormInstance()!!.changedProperty().not())
		displayForm!!.prefWidthProperty().bind(scrollContent!!.prefWidthProperty())
	}

	/**
	 * This method sets up listeners and sets the text of the state change
	 * labels.
	 */
	override fun setupValueChangedListeners()
	{
//		model.getFormInstance()!!.changedProperty()
//			.addListener { observable : ObservableValue<out Boolean>?, oldValue : Boolean?, newValue : Boolean ->
//				changedLabel!!.text = "The form has " + (if (newValue) "" else "not ") + "changed."
//			}
//		model.getFormInstance()!!.validProperty()
//			.addListener { observable : ObservableValue<out Boolean>?, oldValue : Boolean?, newValue : Boolean ->
//				validLabel!!.text = "The form is " + (if (newValue) "" else "not ") + "valid."
//			}
//		model.getFormInstance()!!.persistableProperty()
//			.addListener { observable : ObservableValue<out Boolean>?, oldValue : Boolean?, newValue : Boolean ->
//				persistableLabel!!.text = "The form is " + (if (newValue) "" else "not ") + "persistable."
//			}
//		model.getCountry().nameProperty()
//			.addListener { observable : ObservableValue<out String>?, oldValue : String?, newValue : String ->
//				countryLabel!!.text = "Country: $newValue"
//			}
//		model.getCountry().currencyShortProperty()
//			.addListener { observable : ObservableValue<out String>?, oldValue : String?, newValue : String ->
//				currencyLabel!!.text = "Currency: $newValue"
//			}
//		model.getCountry().populationProperty()
//			.addListener { observable : ObservableValue<out Number>?, oldValue : Number?, newValue : Number ->
//				populationLabel!!.text = "Population: $newValue"
//			}
	}

	/**
	 * This method sets up the handling for all the button clicks.
	 */
	override fun setupEventHandlers()
	{
//		reset!!.onAction = EventHandler { event : ActionEvent? ->
//			model.getFormInstance()!!.reset()
//		}
//		save!!.onAction = EventHandler { event : ActionEvent? ->
//			model.getFormInstance()!!.persist()
//		}
//		languageDE!!.onAction = EventHandler { event : ActionEvent? ->
//			//model.translate("DE")
//			languageDE!!.isDisable = true
//			languageEN!!.isDisable = false
//		}
//		languageEN!!.onAction = EventHandler { event : ActionEvent? ->
//			//model.translate("EN")
//			languageEN!!.isDisable = true
//			languageDE!!.isDisable = false
//		}
//		sectionToggle!!.onAction = EventHandler { event : ActionEvent? ->
//			model.getFormInstance()!!.groups.stream().filter { s : Group? -> s is Section }.forEach { s : Group ->
//				val sec = s as Section
//				sec.collapse(!sec.isCollapsed)
//			}
//		}
//		editableToggle!!.onAction = EventHandler { event : ActionEvent? ->
//			model.getFormInstance()!!.fields.forEach(Consumer { s : Field<*> ->
//				s.editable(
//					!s.isEditable()
//				)
//			})
//		}
	}

	/**
	 * This method is used to layout the nodes and regions properly.
	 */
	override fun layoutParts()
	{
		scrollContent!!.content = displayForm
		scrollContent!!.isFitToWidth = true
		scrollContent!!.minWidthProperty().bind(displayForm!!.prefWidthProperty())
		//scrollContent!!.styleClass.add("scroll-pane")
//		languageDE!!.styleClass.addAll("flaticon", "lang-button", "lang-button--left")
//		languageEN!!.styleClass.addAll("flaticon", "lang-button", "lang-button--right")
//		languageEN!!.isDisable = true
//		languageDE!!.maxWidth = Double.MAX_VALUE
//		languageEN!!.maxWidth = Double.MAX_VALUE
//		HBox.setHgrow(languageDE, Priority.ALWAYS)
//		HBox.setHgrow(languageEN, Priority.ALWAYS)
		//statusContent!!.padding = Insets(10.0)
//		statusContent!!.children.addAll(validLabel, changedLabel, persistableLabel)
		//statusContent!!.spacing = 10.0
		//statusContent!!.prefWidth = 200.0
		//statusContent!!.styleClass.add("bordered")
		//controls!!.add(statusContent, 0, 1)
//		save!!.maxWidth = Double.MAX_VALUE
//		reset!!.maxWidth = Double.MAX_VALUE
//		HBox.setHgrow(save, Priority.ALWAYS)
//		HBox.setHgrow(reset, Priority.ALWAYS)
//		formButtons!!.children.addAll(reset, save)
//		formButtons!!.padding = Insets(10.0)
//		formButtons!!.spacing = 10.0
//		formButtons!!.prefWidth = 200.0
//		formButtons!!.styleClass.add("bordered")
//		controls!!.add(formButtons, 0, 2)
//		bindingInfo!!.padding = Insets(10.0)
//		bindingInfo!!.children.addAll(countryLabel, currencyLabel, populationLabel)
//		bindingInfo!!.spacing = 10.0
//		bindingInfo!!.prefWidth = 200.0
//		bindingInfo!!.styleClass.add("bordered")
//		controls!!.add(bindingInfo, 0, 3)
//		editableToggle!!.maxWidth = Double.MAX_VALUE
//		sectionToggle!!.maxWidth = Double.MAX_VALUE
//		HBox.setHgrow(editableToggle, Priority.ALWAYS)
//		HBox.setHgrow(sectionToggle, Priority.ALWAYS)
//		toggleContent!!.padding = Insets(10.0)
//		toggleContent!!.children.addAll(editableToggle, sectionToggle)
//		toggleContent!!.spacing = 10.0
//		toggleContent!!.prefWidth = 200.0
//		toggleContent!!.styleClass.add("bordered")
//		controls!!.add(toggleContent, 0, 4)
		//controls!!.prefWidth = 200.0
		//controls!!.styleClass.add("controls")
		center = scrollContent
	//	right = controls
	}

	init
	{
		init()
	}
}