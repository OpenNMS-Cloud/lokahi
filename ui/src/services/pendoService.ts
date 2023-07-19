/**
 * Pendo Service
 * 
 * Pendo offers an API in order to trigger certain events, but this method
 * allows our Pendo team to activate items on their end.
 * Everything they do is based on changes to the page, so we add/remove elements 
 * from the page when we want them to show up.
 */


/**
 * 
 * @param id id to add to div
 */
const addDivToPendoAreaWithID = (id: string) => {
    const pendoArea = document.querySelector('#pendo-area');
    const idWithHash = id.includes('#') ? id : '#' + id;

    if (pendoArea) {
        const existingOption = pendoArea.querySelector(idWithHash)
        if (!existingOption) {
            const newDiv = document.createElement('div');
            newDiv.id = id.replace('#', '');
            newDiv.innerText = '1';
            pendoArea.appendChild(newDiv)
        }
    }
}
/**
 * 
 * @param id for element to remove from pendo area
 */
const removeDivFromPendoAreaWithID = (id: string) => {
    const pendoArea = document.querySelector('#pendo-area');
    const idWithHash = id.includes('#') ? id : '#' + id;
    if (pendoArea) {
        const element = pendoArea.querySelector(idWithHash)
        if (element) {
            element.remove();
        }
    }
}

const minionHelpId = 'minion-help-guide'
export const activateMinionHelpGuide = () => {
    addDivToPendoAreaWithID(minionHelpId)
}

export const disableMinionHelpGuide = () => {
    removeDivFromPendoAreaWithID(minionHelpId)
}

const discoveryHelpId = 'discovery-help-guide'
export const activateDiscoveryHelpGuide = () => {
    addDivToPendoAreaWithID(discoveryHelpId)
}

export const disableDiscoveryHelpGuide = () => {
    removeDivFromPendoAreaWithID(discoveryHelpId)
}