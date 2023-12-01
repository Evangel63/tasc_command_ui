package evangel.tascui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import data.campaign.econ.boggledTools
import data.campaign.econ.conditions.Terraforming_Controller
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager
import lunalib.backend.ui.components.base.LunaUIPlaceholder
import lunalib.backend.ui.components.base.LunaUISphere
import lunalib.backend.ui.components.base.LunaUISprite
import lunalib.lunaUI.panel.LunaBaseCustomPanelPlugin
import org.apache.log4j.LogManager
import org.lwjgl.input.Keyboard
import java.awt.Color

class StaticTooltip(title : String?, condition : MarketConditionAPI?, vararg strings : String) : TooltipCreator {
    private val title : String?
    private val paras : ArrayList<String> = ArrayList()
    private val condition : MarketConditionAPI?

    init {
        this.title = title
        this.condition = condition
        for (string in strings) {
            paras.add(string)
        }
    }

    override fun isTooltipExpandable(tooltipParam : Any?) : Boolean {
        return false
    }

    override fun getTooltipWidth(tooltipParam : Any?) : Float {
        return 500f
    }

    override fun createTooltip(tooltip : TooltipMakerAPI, expanded : Boolean, tooltipParam : Any?) {
        var spacing = 0f
        if (title != null) {
            tooltip.addPara(title, Global.getSector().playerFaction.brightUIColor, 0f)
            spacing = 9f
        }

        for (para in paras) {
            var substitutedPara = para
            if (condition != null && condition.plugin != null) {
                for (sub in condition.plugin!!.tokenReplacements) {
                    substitutedPara = substitutedPara.replace(sub.key, sub.value)
                }
            }
            tooltip.addPara(substitutedPara, spacing)
            spacing = 8f
        }
        spacing = 8f

        if (condition != null) {
            val hazardRating = (condition.genSpec.hazard * 100).toInt()
            if (hazardRating != 0) {
                val hazardPrefix = if (hazardRating > 0) "+" else ""
                tooltip.addPara("$hazardPrefix$hazardRating%% hazard rating", spacing, Misc.getHighlightColor(), "$hazardPrefix$hazardRating%")
            }
        }
    }
}

class ProjectRequirementsTooltip(terraformingOptionID : String, width : Float) : TooltipCreator {
    val terraformingOptionID : String
    private val width : Float
    var market : MarketAPI? = null

    val aotdEnabled : Boolean = Global.getSettings().modManager.isModEnabled("aotd_vok")

    init {
        this.terraformingOptionID = terraformingOptionID
        this.width = width
    }

    override fun isTooltipExpandable(tooltipParam : Any?): Boolean {
        return false
    }

    override fun getTooltipWidth(tooltipParam : Any?) : Float {
        return width
    }

    override fun createTooltip(tooltip : TooltipMakerAPI, expanded : Boolean, tooltipParam : Any?) {
        val projectRequirements = boggledTools.getProjectRequirementsStrings(terraformingOptionID)

        for (projectRequirement in projectRequirements) {
            val requirementMet = boggledTools.requirementMet(market!!, projectRequirement)
            val color = if (requirementMet) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
            tooltip.addPara(projectRequirement, color, 0f)
        }

        if (aotdEnabled) {
            val good = Misc.getPositiveHighlightColor()
            val bad = Misc.getNegativeHighlightColor()
            val researchManager = AoTDMainResearchManager.getInstance()
            val researchReqPara = CommandUIIntelK.getRequiredResearchParaFromProjectName(terraformingOptionID)
            val researchReq = CommandUIIntelK.getRequiredResearchFromProjectName(terraformingOptionID)
            val color = if (researchManager.isResearchedForPlayer(researchReq)) good else bad
            tooltip.addPara(researchReqPara, color, 0f)
        }
    }
}

class PlanetCardTooltip(basePanel : CustomPanelAPI, market : MarketAPI) : TooltipCreator {
    private val basePanel : CustomPanelAPI
    private val market : MarketAPI

    init {
        this.basePanel = basePanel
        this.market = market
    }

    override fun isTooltipExpandable(tooltipParam : Any?): Boolean {
        return false;
    }

    override fun getTooltipWidth(tooltipParam : Any?): Float {
        return 450f
    }

    override fun createTooltip(tooltip : TooltipMakerAPI, expanded : Boolean, tooltipParam : Any?) {
        tooltip.addSectionHeading("Industries & structures", Alignment.MID, 0f)

        val industriesPlaceholder = LunaUIPlaceholder(false, getTooltipWidth(null), 400f, "", "PlanetCardTooltip", basePanel, tooltip)
        industriesPlaceholder.position!!.inTL(0f, 0f)

        val industriesElement = industriesPlaceholder.lunaElement!!.createUIElement(getTooltipWidth(null), 400f, false)
        industriesElement.position.inTL(0f, 0f)

        industriesPlaceholder.lunaElement!!.addUIElement(industriesElement)

        var horizontalSpacing = 0f
        var verticalSpacing = 0f
        for (industry in market.industries) {
            val industryHolder = LunaUIPlaceholder(false, getTooltipWidth(null) / 2, CommandUIIntelK.PLANET_CARD_HEIGHT / 2, "", "PlanetCardTooltipItem", industriesPlaceholder.lunaElement!!, industriesElement)
            industryHolder.position!!.inTL(horizontalSpacing, verticalSpacing)

            val industryElement = industryHolder.lunaElement!!.createUIElement(getTooltipWidth(null) / 2, CommandUIIntelK.PLANET_CARD_HEIGHT / 2, false)
            industryElement.position.inTL(0f, 0f)

            industryHolder.lunaElement!!.addUIElement(industryElement)

            val sprite = LunaUISprite(industry.spec.imageName, 20f, 20f, 0f, 0f, 0f, 0f, "", "IndustrySprite", industryHolder.lunaElement!!, industryElement)

            horizontalSpacing += getTooltipWidth(null) / 2
            if (horizontalSpacing >= getTooltipWidth(null)) {
                horizontalSpacing = 0f
                verticalSpacing += 20f
            }
        }
    }
}

data class CommandUIButtonData(val planetCard : ButtonAPI, val market : MarketAPI, val uiIntelK : CommandUIIntelK, var projectLabel : LabelAPI? = null, var projectTimeRemaining : LabelAPI? = null) {
}

data class CommandUITerraformingButtonPanelData(val planetNameLabel : LabelAPI) {
}

class CommandUIIntelK : LunaBaseCustomPanelPlugin() {
    private var LOGGER = LogManager.getLogger("evangel.tascui.CommandUIIntelK")

    private var width = 0f
    private var height = 0f

    /*
    Holds buttons for each state regardless of if the planet meets the requirements
    Buttons are moved offscreen (inTL(100000f, 100000f)) when not being used
     */
    private var requirementsMetButtons : ArrayList<ButtonAPI> = ArrayList()
    private var requirementsNotMetButtons : ArrayList<ButtonAPI> = ArrayList()

    private var startProjectButton : ButtonAPI? = null
    private var requirementsNotMetButton : ButtonAPI? = null

    private var activeCancelProjectButton : ButtonAPI? = null
    private var inactiveCancelProjectButton : ButtonAPI? = null

    private var selectedProject : ButtonAPI? = null
    private var selectedPlanet : CommandUIButtonData? = null

    private var terraformingPanelData : CommandUITerraformingButtonPanelData? = null

    private var aotdEnabled : Boolean = Global.getSettings().modManager.isModEnabled("aotd_vok")

    companion object {
        private const val HEADER_HEIGHT = 20f

        private const val SORT_SPACING = 3f

        private const val NAME_WIDTH = 170f
        private const val NAME_OFFSET = 0f
        private const val CONDITIONS_WIDTH = 200f
        private const val CONDITIONS_OFFSET = NAME_OFFSET + NAME_WIDTH + SORT_SPACING
        private const val HAZARD_WIDTH  = 150f
        private const val HAZARD_OFFSET = CONDITIONS_OFFSET + CONDITIONS_WIDTH + SORT_SPACING

        private const val SORT_PANEL_WIDTH = HAZARD_OFFSET + HAZARD_WIDTH
        private const val SORT_PANEL_HEIGHT = HEADER_HEIGHT
        private const val SORT_ARROW_SPRITE_SIZE = 15f

        private const val PLANET_MOON_SPRITE_SIZE = 15f
        private const val PLANET_PLANET_SPRITE_SIZE = 20f
        private const val PLANET_GAS_GIANT_SPRITE_SIZE = 30f

        private const val PLANETS_PANEL_WIDTH = SORT_PANEL_WIDTH

        private const val PLANET_CARD_WIDTH = PLANETS_PANEL_WIDTH
        const val PLANET_CARD_HEIGHT = 80f

        private const val CONDITION_SPRITE_SIZE = 21f
        private const val CONDITION_SPRITE_SIZE_W_SPACING = CONDITION_SPRITE_SIZE + 3f

        private const val PLANET_CARD_HOLDER_MAGIC_X_PAD = 5f
        private const val PLANET_TYPE_LABEL_MAGIC_X_PAD = -4f

        fun getRequiredResearchParaFromProjectName(projectName : String) : String {
            var researchReqPara = ""
            if (projectName.contains("TypeChange")) researchReqPara = "Researched : Terraforming Templates"
            else if (projectName.contains("ConditionImprovement")) researchReqPara = "Researched : Atmosphere Manipulation"
            else if (projectName.contains("ResourceImprovement")) researchReqPara = "Researched : Advanced Terraforming Templates"
            return researchReqPara
        }

        fun getRequiredResearchFromProjectName(projectName : String) : String {
            var researchReq = ""
            if (projectName.contains("TypeChange")) researchReq = "tasc_terraforming_templates"
            else if (projectName.contains("ConditionImprovement")) researchReq = "tasc_atmosphere_manipulation"
            else if (projectName.contains("ResourceImprovement")) researchReq = "tasc_advacned_terraforming"
            return researchReq
        }

        private fun getTerraformingOptionPair(projectName : String) : Triple<String, String, String> {
            val researchReq = getRequiredResearchFromProjectName(projectName)
            return Triple(projectName, boggledTools.getTooltipProjectName(projectName), researchReq)
        }

        private val BOGGLED_TERRAFORMING_OPTIONS = arrayOf(
//            getTerraformingOptionPair("irradiatedConditionImprovement"),
            Triple("irradiatedConditionImprovement", "Remove atmospheric radiation", "tasc_atmosphere_manipulation"),
            getTerraformingOptionPair("toxicAtmosphereConditionImprovement"),
            getTerraformingOptionPair("atmosphereDensityConditionImprovement"),
            getTerraformingOptionPair("habitableConditionImprovement"),
            getTerraformingOptionPair("mildClimateConditionImprovement"),
            getTerraformingOptionPair("extremeWeatherConditionImprovement"),
            getTerraformingOptionPair("volatilesResourceImprovement"),
            getTerraformingOptionPair("organicsResourceImprovement"),
            getTerraformingOptionPair("farmlandResourceImprovement"),
            getTerraformingOptionPair("waterTypeChange"),
            getTerraformingOptionPair("tundraTypeChange"),
            getTerraformingOptionPair("terranTypeChange"),
            getTerraformingOptionPair("jungleTypeChange"),
            getTerraformingOptionPair("frozenTypeChange"),
            getTerraformingOptionPair("aridTypeChange")
        )

        private const val BUTTON_OFF_SCREEN_POSITION = 100000f

        private fun getTerraformingControllerFromMarket(market : MarketAPI) : Terraforming_Controller {
            return market.getCondition("terraforming_controller").plugin as Terraforming_Controller
        }

        private fun getTerraformingDaysRemaining(terraformingController: Terraforming_Controller) : Int {
            if (terraformingController.project == null || terraformingController.project == "None") return 0
            return terraformingController.daysRemaining
        }

        private fun getTerraformingDaysRemainingComplete(terraformingController : Terraforming_Controller) : String {
            if (terraformingController.project == null || terraformingController.project == "None") return ""
            val daysRemaining = terraformingController.daysRemaining
            val days = if (daysRemaining == 1) " day " else " days "
            return daysRemaining.toString() + days + "remaining"
        }
    }

    override fun init() {
        enableCloseButton = true
        width = panel.position.width
        height = panel.position.height

        val element = panel.createUIElement(width, HEADER_HEIGHT, false)
        element.addSectionHeading("Terraforming", Alignment.MID, 0f)
        element.position.inTL(0f, 0f)
        panel.addUIElement(element)

        createPlanetList()

        createTerraformingSelection()
    }

    override fun processInput(events : MutableList<InputEventAPI>) {
        super.processInput(events)

        events.forEach { event ->
            if (event.isKeyDownEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                event.consume()
                close()
                return@forEach
            }
        }
    }

    private fun createTerraformingSelectionButtons(basePanel : CustomPanelAPI, baseElement : TooltipMakerAPI, width : Float, height : Float, yPad : Float) {
        val faction = Global.getSector().playerFaction

        val buttonsHolder = LunaUIPlaceholder(false, width, height, "", "TerraformingButtons", basePanel, baseElement)
        buttonsHolder.position!!.inTL(0f, yPad)

        val buttonsElement = buttonsHolder.lunaElement!!.createUIElement(width, height, true)
        buttonsElement.position.inTL(0f, 0f)

        buttonsHolder.lunaElement!!.addUIElement(buttonsElement)

        for (terraformingOption in BOGGLED_TERRAFORMING_OPTIONS) {
            val projectRequirementsTooltip = ProjectRequirementsTooltip(terraformingOption.first, width)

            val validButton = buttonsElement.addButton(terraformingOption.second, projectRequirementsTooltip, faction.baseUIColor, faction.darkUIColor, Alignment.LMID, CutStyle.ALL, width, HEADER_HEIGHT, 0f)
            val invalidButton = buttonsElement.addButton(terraformingOption.second, projectRequirementsTooltip, faction.baseUIColor.darker(), faction.darkUIColor.darker(), Alignment.LMID, CutStyle.ALL, width, HEADER_HEIGHT, 0f)

            buttonsElement.addTooltipTo(projectRequirementsTooltip, validButton, TooltipMakerAPI.TooltipLocation.ABOVE)
            buttonsElement.addTooltipTo(projectRequirementsTooltip, invalidButton, TooltipMakerAPI.TooltipLocation.ABOVE)

            validButton.position!!.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
            invalidButton.position!!.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)

            requirementsMetButtons.add(validButton)
            requirementsNotMetButtons.add(invalidButton)
        }
    }

    private fun createTerraformingSelection() {
        val verticalSpacing = HEADER_HEIGHT + 1f

        val terraformingPanelWidth = width - SORT_PANEL_WIDTH - 9f
        val terraformingPanelHeight = height - verticalSpacing - 5f

        val terraformingPanel = panel.createCustomPanel(terraformingPanelWidth, terraformingPanelHeight, null)
        terraformingPanel.position.inTR(2f, verticalSpacing)

        val terraformingElement = terraformingPanel.createUIElement(terraformingPanelWidth, terraformingPanelHeight, true)
        terraformingElement.position.inTL(0f, 0f)

        terraformingPanel.addUIElement(terraformingElement)
        panel.addComponent(terraformingPanel)

        val headerHolder = LunaUIPlaceholder(false, terraformingPanelWidth, terraformingPanelHeight, "", "TerraformingSectionHeader", terraformingPanel, terraformingElement)
        headerHolder.position!!.inTL(0f, 0f)

        val headerElement = headerHolder.lunaElement!!.createUIElement(terraformingPanelWidth, terraformingPanelHeight, false)
        val headerLabel = headerElement.addSectionHeading(selectedPlanet?.market?.name, Alignment.MID, 0f)

        headerHolder.lunaElement!!.addUIElement(headerElement)

        val buttonsWidth = terraformingPanelWidth * 0.5f;
        val buttonsHeight = terraformingPanelHeight - verticalSpacing
        createTerraformingSelectionButtons(terraformingPanel, terraformingElement, buttonsWidth, buttonsHeight, verticalSpacing)

        val otherButtonsWidth = terraformingPanelWidth - buttonsWidth - SORT_SPACING
        val otherButtonsHeight = buttonsHeight
        val otherButtonsHolder = LunaUIPlaceholder(false, otherButtonsWidth, otherButtonsHeight, "", "TerraformingInitiateHolder", terraformingPanel, terraformingElement)
        otherButtonsHolder.position!!.inTL(buttonsWidth + SORT_SPACING -1f, verticalSpacing)

        val otherButtonsElement = otherButtonsHolder.lunaElement!!.createUIElement(otherButtonsWidth, otherButtonsHeight, false)
        otherButtonsElement.position!!.inTL(0f, 0f)

        otherButtonsHolder.lunaElement!!.addUIElement(otherButtonsElement)

        val faction = Global.getSector().playerFaction

        startProjectButton = otherButtonsElement.addButton("Start project", null, faction.baseUIColor, faction.darkUIColor, Alignment.MID, CutStyle.ALL, otherButtonsWidth, HEADER_HEIGHT, 0f)
        requirementsNotMetButton = otherButtonsElement.addButton("Requirements not met", null, faction.baseUIColor.darker(), faction.darkUIColor.darker(), Alignment.MID, CutStyle.ALL, otherButtonsWidth, HEADER_HEIGHT, 0f)

        requirementsNotMetButton!!.setClickable(false)

        startProjectButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
        requirementsNotMetButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)

        val cancelProjectButtonBR = Pair(0f, otherButtonsHeight - HEADER_HEIGHT)
        activeCancelProjectButton = otherButtonsElement.addButton("Cancel current project", cancelProjectButtonBR, faction.baseUIColor, faction.darkUIColor, Alignment.MID, CutStyle.ALL, otherButtonsWidth, HEADER_HEIGHT, 0f)
        inactiveCancelProjectButton = otherButtonsElement.addButton("Cancel current project", cancelProjectButtonBR, faction.baseUIColor.darker(), faction.darkUIColor.darker(), Alignment.MID, CutStyle.ALL, otherButtonsWidth, HEADER_HEIGHT, 0f)

        inactiveCancelProjectButton!!.setClickable(false)

        activeCancelProjectButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
        inactiveCancelProjectButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)

        terraformingPanelData = CommandUITerraformingButtonPanelData(headerLabel)
    }

    private fun updateTerraformingSelection() {
        if (terraformingPanelData != null && selectedPlanet != null) {
            terraformingPanelData!!.planetNameLabel.text = selectedPlanet!!.market.name

            selectedProject?.unhighlight()
            selectedProject = null

            for (button in requirementsMetButtons) {
                button.position!!.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
            }
            for (button in requirementsNotMetButtons) {
                button.position!!.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
            }

            startProjectButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
            requirementsNotMetButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)

            val validTerraformingOptions : ArrayList<Triple<String, String, String>> = ArrayList()
            val invalidTerraformingOptions : ArrayList<Triple<String, String, String>> = ArrayList()
            for (terraformingOption in BOGGLED_TERRAFORMING_OPTIONS) {
                var researched = true
                if (aotdEnabled) {
                    researched = AoTDMainResearchManager.getInstance().isResearchedForPlayer(terraformingOption.third)
                }
                if (boggledTools.projectRequirementsMet(selectedPlanet!!.market, terraformingOption.first) && researched) {
                    validTerraformingOptions.add(terraformingOption)
                } else {
                    invalidTerraformingOptions.add(terraformingOption)
                }
            }

            var buttonVerticalSpacing = 0f
            val positionButtons = { terraformingOptions : ArrayList<Triple<String, String, String>>, buttons : ArrayList<ButtonAPI> ->
                for (terraformingOption in terraformingOptions) {
                    val button = buttons.find { (it.customData as ProjectRequirementsTooltip).terraformingOptionID == terraformingOption.first }
                    if (button != null) {
                        val projectRequirementsTooltip = button.customData as ProjectRequirementsTooltip
                        button.position.inTL(0f, buttonVerticalSpacing)
                        projectRequirementsTooltip.market = selectedPlanet?.market
                    }

                    buttonVerticalSpacing += HEADER_HEIGHT + SORT_SPACING
                }
            }
            positionButtons(validTerraformingOptions, requirementsMetButtons)
            positionButtons(invalidTerraformingOptions, requirementsNotMetButtons)
        }
    }

    private fun moveCancelButtonOffscreen(activeButton : ButtonAPI, inactiveButton : ButtonAPI) {
        val p = activeButton.customData as Pair<*, *>
        val x = p.first as Float
        val y = p.second as Float
        activeButton.position.inTR(x, y)
        inactiveButton.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
    }

    private fun handlePlanetCardPress(data : CommandUIButtonData) {
        if (selectedPlanet != null && selectedPlanet !== data) {
            selectedPlanet!!.planetCard.isChecked = false
            selectedPlanet!!.planetCard.unhighlight()
        }

        selectedPlanet = data
        selectedPlanet!!.planetCard.isChecked = !selectedPlanet!!.planetCard.isChecked
        if (selectedPlanet!!.planetCard.isChecked) selectedPlanet!!.planetCard.highlight()
        else selectedPlanet!!.planetCard.unhighlight()

        if (selectedPlanet != null) {
            updateTerraformingSelection()

            val terraformingController = getTerraformingControllerFromMarket(selectedPlanet!!.market)
            if (terraformingController.project == "None") {
                moveCancelButtonOffscreen(inactiveCancelProjectButton!!, activeCancelProjectButton!!)
            } else {
                moveCancelButtonOffscreen(activeCancelProjectButton!!, inactiveCancelProjectButton!!)
            }
        }
    }

    private fun handleTerraformingOptionButtonPress() {
        val projectID = (selectedProject?.customData as ProjectRequirementsTooltip).terraformingOptionID
        if (boggledTools.projectRequirementsMet(selectedPlanet?.market, projectID)) {
            startProjectButton!!.position.inTL(0f, 0f)
            requirementsNotMetButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
        } else {
            startProjectButton!!.position.inTL(BUTTON_OFF_SCREEN_POSITION, BUTTON_OFF_SCREEN_POSITION)
            requirementsNotMetButton!!.position.inTL(0f, 0f)
        }
    }

    private fun handleTerraformingStartProjectButtonPress() {
        val terraformingController = getTerraformingControllerFromMarket(selectedPlanet!!.market)

        val terraformingOptionID = (selectedProject?.customData as ProjectRequirementsTooltip).terraformingOptionID

        terraformingController.project = terraformingOptionID

        selectedPlanet?.projectLabel?.text = boggledTools.getTooltipProjectName(terraformingOptionID)
        selectedPlanet?.projectTimeRemaining?.text = getTerraformingDaysRemainingComplete(terraformingController)
        selectedPlanet?.projectTimeRemaining?.setHighlight("${getTerraformingDaysRemaining(terraformingController)}")

        moveCancelButtonOffscreen(activeCancelProjectButton!!, inactiveCancelProjectButton!!)
    }

    private fun handleTerraformingCancelProjectButtonPress() {
        val terraformingController = getTerraformingControllerFromMarket(selectedPlanet!!.market)
        terraformingController.project = "None"

        selectedPlanet?.projectLabel?.text = "None"
        selectedPlanet?.projectTimeRemaining?.text = ""

        moveCancelButtonOffscreen(inactiveCancelProjectButton!!, activeCancelProjectButton!!)
    }

    private fun createSortButton(width : Float, height : Float, basePanel : CustomPanelAPI, baseElement : TooltipMakerAPI, xPad : Float, buttonText : String, data : Any?, tooltip : StaticTooltip) {
        val faction = Global.getSector().playerFaction

        val holder = LunaUIPlaceholder(false, width, height, "", "Sorting", basePanel, baseElement)
        holder.position!!.inTL(xPad, 0f)

        val element = holder.lunaElement!!.createUIElement(width, height, false)
        val button = element.addAreaCheckbox(buttonText, data, faction.baseUIColor, faction.darkUIColor, faction.brightUIColor, width, height, 0f)
        element.position!!.inTL(0f, 0f)

        button.setClickable(false)

        element.addTooltipToPrevious(tooltip, TooltipMakerAPI.TooltipLocation.ABOVE)

        val arrowSpritePath = "graphics/ui/buttons/arrow_down2.png"
        val arrowSprite = LunaUISprite(arrowSpritePath, SORT_ARROW_SPRITE_SIZE, SORT_ARROW_SPRITE_SIZE, 0f, 0f, 0f, 0f, "", "Sorting", basePanel, element)
        arrowSprite.position!!.inRMid(0f)
        arrowSprite.sprite.color = faction.baseUIColor

        holder.lunaElement!!.addUIElement(element)
    }

    private fun createSortButtons(basePanel : CustomPanelAPI, yPad : Float) {
        val sortPanel = basePanel.createCustomPanel(SORT_PANEL_WIDTH, SORT_PANEL_HEIGHT, null)
        sortPanel.position.inTL(0f, yPad)

        val sortPanelElement = sortPanel.createUIElement(SORT_PANEL_WIDTH, SORT_PANEL_HEIGHT, false)
        sortPanelElement.position.inTL(0f, 0f)

        sortPanel.addUIElement(sortPanelElement)

        basePanel.addComponent(sortPanel)

        var horizontalSpacing = NAME_OFFSET
        createSortButton(NAME_WIDTH, SORT_PANEL_HEIGHT, sortPanel, sortPanelElement, horizontalSpacing, "Name", null, StaticTooltip(null, null, "Colony name."))//, "Sorts colonies by date established."))
        horizontalSpacing = CONDITIONS_OFFSET
        createSortButton(CONDITIONS_WIDTH, SORT_PANEL_HEIGHT, sortPanel, sortPanelElement, horizontalSpacing, "Conditions", null, StaticTooltip(null, null, "Planetary/local conditions. Does not include conditions added by human activity."))//, "Sorts by number of conditions."))
        horizontalSpacing = HAZARD_OFFSET
        createSortButton(HAZARD_WIDTH, SORT_PANEL_HEIGHT, sortPanel, sortPanelElement, horizontalSpacing, "Hazard rating", null, StaticTooltip(null, null, "Planetary hazard rating."))//, "Sorts by hazard rating."))
    }

    private fun createPlanetsNamePanel(basePanel : CustomPanelAPI, baseElement : TooltipMakerAPI, market : MarketAPI, faction : FactionAPI) {
        /*
        Stack SHOULD be as follows: nameHolder -> nameElement -> nameSprite -> nameLabel
         */
        val nameHolder = LunaUIPlaceholder(false, NAME_WIDTH, PLANET_CARD_HEIGHT, "", "PlanetName", basePanel, baseElement)
        nameHolder.lunaElement!!.position.inTL(NAME_OFFSET, 0f)
        val nameElement = nameHolder.lunaElement!!.createUIElement(NAME_WIDTH, PLANET_CARD_HEIGHT, false)
        nameElement.position.inTL(0f, 0f)

        nameHolder.lunaElement!!.addUIElement(nameElement)

        var nameSpriteSize = PLANET_PLANET_SPRITE_SIZE
        if (market.planetEntity.isMoon) nameSpriteSize = PLANET_MOON_SPRITE_SIZE
        if (market.planetEntity.isGasGiant) nameSpriteSize = PLANET_GAS_GIANT_SPRITE_SIZE

        val nameSprite = LunaUISphere(market.planetEntity.spec.texture, nameSpriteSize, 0f, 0f, "", "Planets", nameHolder.lunaElement!!, nameElement)
        nameSprite.position!!.inTL(NAME_WIDTH / 2, PLANET_CARD_HEIGHT / 2)

        val nameLabelElement = nameHolder.lunaElement!!.createUIElement(NAME_WIDTH, HEADER_HEIGHT, false)
        val nameLabel = nameLabelElement.addPara(market.name, faction.baseUIColor, 0f)
        val nameLength = nameLabel.computeTextWidth(market.name)
        nameLabelElement.position.inBL(NAME_WIDTH / 2 - nameLength / 2, 0f)
        nameHolder.lunaElement!!.addUIElement(nameLabelElement)
    }

    private fun createPlanetsConditionPanel(basePanel : CustomPanelAPI, baseElement : TooltipMakerAPI, market : MarketAPI) {
        /*
        stack SHOULD be: conditionHolder -> conditionElement -> conditionSprites
         */
        val conditionHolder = LunaUIPlaceholder(false, CONDITIONS_WIDTH, PLANET_CARD_HEIGHT, "", "PlanetConditions", basePanel, baseElement)
        conditionHolder.lunaElement!!.position.inTL(CONDITIONS_OFFSET, 0f)
        val conditionElement = conditionHolder.lunaElement!!.createUIElement(CONDITIONS_WIDTH, PLANET_CARD_HEIGHT, false)
        conditionElement.position.inTL(0f, 0f)

        conditionHolder.lunaElement!!.addUIElement(conditionElement)

        var conditionHorizontalSpacing = 0f
        var conditionVerticalSpacing = PLANET_CARD_HEIGHT / 2
        val rows = (CONDITIONS_WIDTH / (market.conditions.size * CONDITION_SPRITE_SIZE_W_SPACING)).toInt()

        var conditionSpriteSize = CONDITION_SPRITE_SIZE
        val conditionSpriteCapacity = PLANET_CARD_HEIGHT / 2
        val conditionSpriteSpacing = 6f
        if ((rows * conditionSpriteSize) > conditionSpriteCapacity) {
            conditionSpriteSize = conditionSpriteCapacity / (rows + rows * conditionSpriteSpacing)
        }

        val planetTypeLabelElement = conditionHolder.lunaElement!!.createUIElement(CONDITIONS_WIDTH, HEADER_HEIGHT, false)
        val planetTypeLabel = planetTypeLabelElement.addPara(market.planetEntity.typeNameWithWorld, market.planetEntity.lightColor, 0f)
        val planetTypeHeight = planetTypeLabel.computeTextHeight(market.planetEntity.typeNameWithWorld)
        planetTypeLabelElement.position.inTL(PLANET_TYPE_LABEL_MAGIC_X_PAD, conditionVerticalSpacing - planetTypeHeight - 5f)
        conditionHolder.lunaElement!!.addUIElement(planetTypeLabelElement)

        for (condition in market.conditions) {
            if (!condition.isPlanetary) continue

            val conditionBorderSpriteName = "graphics/ui/buttons/pause.png"
            val conditionBorderSprite = LunaUISprite(conditionBorderSpriteName, conditionSpriteSize, conditionSpriteSize, 0f, 0f, 0f, 0f, "", "PlanetConditionsSprite", conditionHolder.lunaElement!!, conditionElement)
            conditionBorderSprite.position!!.inTL(conditionHorizontalSpacing, conditionVerticalSpacing)
            conditionBorderSprite.sprite.color = Global.getSector().playerFaction.baseUIColor

            val conditionSprite = LunaUISprite(condition.spec.icon, conditionSpriteSize, conditionSpriteSize, 0f, 0f, 0f, 0f, "", "PlanetConditionsSprite", conditionHolder.lunaElement!!, conditionElement)
            conditionSprite.position!!.inTL(conditionHorizontalSpacing, conditionVerticalSpacing)

            val ttName = condition.spec.name
            val ttDescription = condition.spec.desc
            for (replacement in condition.plugin.tokenReplacements) {
                ttDescription.replace(replacement.key, replacement.value)
            }

            conditionElement.addTooltipTo(StaticTooltip(ttName, condition, ttDescription), conditionSprite.lunaElement, TooltipMakerAPI.TooltipLocation.BELOW)

            conditionHorizontalSpacing += conditionSpriteSize + conditionSpriteSpacing
            if (conditionHorizontalSpacing > CONDITIONS_WIDTH) {
                conditionHorizontalSpacing = 0f
                conditionVerticalSpacing += conditionSpriteSize + conditionSpriteSpacing
            }
        }
    }

    private fun createPlanetsHazardPanel(basePanel : CustomPanelAPI, baseElement : TooltipMakerAPI, market : MarketAPI, faction : FactionAPI, buttonData : CommandUIButtonData) {
        /*
        stack SHOULD be: hazardHolder -> hazardElement -> hazardText
         */
        val hazardHolder = LunaUIPlaceholder(false, HAZARD_WIDTH, PLANET_CARD_HEIGHT, "", "PlanetHazard", basePanel, baseElement)
        hazardHolder.lunaElement!!.position.inTL(HAZARD_OFFSET, 0f)
        val hazardElement = hazardHolder.lunaElement!!.createUIElement(HAZARD_WIDTH, PLANET_CARD_HEIGHT, false)
        hazardElement.position.inTL(0f, 0f)

        val hazardLabelElement = hazardHolder.lunaElement!!.createUIElement(HAZARD_WIDTH, HEADER_HEIGHT, false)
        val hazardLabelRating = (market.hazard.modified * 100).toInt()
        val hazardLabel = hazardLabelElement.addPara("Hazard rating: $hazardLabelRating%%", 0f, faction.baseUIColor, Misc.getHighlightColor(), "$hazardLabelRating%")
        hazardLabelElement.position.inBL(0f, 0f)
        hazardHolder.lunaElement!!.addUIElement(hazardLabelElement)

        val terraformingController = getTerraformingControllerFromMarket(market)
        val projectLabelElement = hazardHolder.lunaElement!!.createUIElement(HAZARD_WIDTH, HEADER_HEIGHT * 2, false)
        val projectNameNicer = boggledTools.getTooltipProjectName(terraformingController.project)
        buttonData.projectLabel = projectLabelElement.addPara(projectNameNicer, faction.baseUIColor, 0f)
        projectLabelElement.position.inTL(0f, SORT_SPACING)

        val projectTimeRemainingElement = hazardHolder.lunaElement!!.createUIElement(HAZARD_WIDTH, HEADER_HEIGHT, false)
        buttonData.projectTimeRemaining = projectTimeRemainingElement.addPara(getTerraformingDaysRemainingComplete(terraformingController), 0f, faction.baseUIColor, Misc.getHighlightColor(), "${getTerraformingDaysRemaining(terraformingController)}")
        projectTimeRemainingElement.position.inBL(0f, HEADER_HEIGHT + SORT_SPACING)

        buttonData.projectTimeRemaining?.position?.inBL(SORT_SPACING, 0f)

        hazardHolder.lunaElement!!.addUIElement(projectLabelElement)
        hazardHolder.lunaElement!!.addUIElement(projectTimeRemainingElement)

        hazardHolder.lunaElement!!.addUIElement(hazardElement)
    }

    private fun createPlanetsPanel(basePanel : CustomPanelAPI, height : Float, yPad : Float) {
        val faction = Global.getSector().playerFaction
        val markets : ArrayList<MarketAPI> = boggledTools.getNonStationMarketsPlayerControls()

        val planetsPanelWidth = PLANETS_PANEL_WIDTH
        val planetsPanelHeight = height - yPad - 5f

        val planetsPanel = basePanel.createCustomPanel(planetsPanelWidth, planetsPanelHeight, null)
        planetsPanel.position.inTL(0f, yPad)

        val planetsElement = planetsPanel.createUIElement(planetsPanelWidth, planetsPanelHeight, true)
        planetsElement.position.inTL(0f, 0f)

        if (markets.isEmpty()) {
            planetsElement.addAreaCheckbox("No planets", null, Color(0, 0, 0, 0), Color(0, 0, 0, 0), faction.brightUIColor, planetsPanelWidth, planetsPanelHeight, 0f)
        }

        planetsPanel.addUIElement(planetsElement)

        basePanel.addComponent(planetsPanel)

        var verticalSpacing = 3f
        for (marketVar in markets) {
            /*
            cardHolder is the placeholder for the entire planet's panel
            cardElement is the TooltipMakerAPI for the entire planet's panel
            Stack SHOULD be as follows: cardHolder -> cardElement -> nameHolder | conditionHolder | hazardHolder
             */
            val cardHolder = LunaUIPlaceholder(false, PLANET_CARD_WIDTH, PLANET_CARD_HEIGHT, "", "Planets", planetsPanel, planetsElement)
            cardHolder.position!!.inTL(PLANET_CARD_HOLDER_MAGIC_X_PAD, verticalSpacing)

            val cardElement = cardHolder.lunaElement!!.createUIElement(PLANET_CARD_WIDTH, PLANET_CARD_HEIGHT, false)
            cardElement.position!!.inTL(0f, 0f)

            val button = cardElement.addAreaCheckbox(null, null, faction.baseUIColor, Color(0,0, 0,0), faction.brightUIColor, PLANET_CARD_WIDTH, PLANET_CARD_HEIGHT, 0f)
            button.position.inTL(0f, 0f)

            val buttonData = CommandUIButtonData(button, marketVar, this)

            createPlanetsNamePanel(cardHolder.lunaElement!!, cardElement, marketVar, faction)

            createPlanetsConditionPanel(cardHolder.lunaElement!!, cardElement, marketVar)

            createPlanetsHazardPanel(cardHolder.lunaElement!!, cardElement, marketVar, faction, buttonData)

            val actualButton = LunaUIPlaceholder(false, PLANET_CARD_WIDTH, PLANET_CARD_HEIGHT, buttonData, "Planets", planetsPanel, planetsElement)
            actualButton.position!!.inTL(0f, verticalSpacing)

//            cardElement.addTooltipTo(PlanetCardTooltip(cardHolder.lunaElement!!, marketVar), button, TooltipMakerAPI.TooltipLocation.RIGHT)

            /*
            handlePlanetCardPress is where everything related to actually selecting a planetCard is handled
             */
            actualButton.onClick {
                handlePlanetCardPress(this.key as CommandUIButtonData)
            }

            cardHolder.lunaElement!!.addUIElement(cardElement)

            verticalSpacing += PLANET_CARD_HEIGHT
        }
    }

    private fun createPlanetList() {
        var verticalSpacing = HEADER_HEIGHT
        createSortButtons(panel, verticalSpacing)

        verticalSpacing += SORT_PANEL_HEIGHT

        val planetsPanelHeight = height - verticalSpacing - 5f
        createPlanetsPanel(panel, planetsPanelHeight, verticalSpacing)
    }

    override fun advance(amount : Float) {
        val updateTerraformingButtons = { buttons : ArrayList<ButtonAPI> ->
            for (button in buttons) {
                if (button.isChecked) {
                    selectedProject?.unhighlight()
                    selectedProject = button
                    button.isChecked = false
                    button.highlight()

                    handleTerraformingOptionButtonPress()
                }
            }
        }
        updateTerraformingButtons(requirementsMetButtons)
        updateTerraformingButtons(requirementsNotMetButtons)

        if (startProjectButton?.isChecked == true) {
            startProjectButton!!.isChecked = false

            handleTerraformingStartProjectButtonPress()
        }

        if (activeCancelProjectButton?.isChecked == true) {
            activeCancelProjectButton!!.isChecked = false

            handleTerraformingCancelProjectButtonPress()
        }
    }
}