import * as React from 'react';
import Collapsible from '../../shared_ui/collapsible';
import ImmutableObjectUtils from '../../lib/immutable_object_utils';
import FormInput, {FocusableTextInputInterface} from '../../form/input';
import Textarea from '../../form/textarea';
import Formatter from '../../lib/formatter';
import autobind from "../../lib/autobind";
import {EnvironmentVariableData, EnvironmentVariablesData} from "./loader";
import Button from "../../form/button";
import {DataRequest, ResponseError} from "../../lib/data_request";
import DynamicLabelButton from "../../form/dynamic_label_button";
import ConfirmActionPanel from "../../panels/confirm_action";
import FormSearch from "../../form/search";
import SubstringHighlighter from "../../shared_ui/substring_highlighter";

const formatEnvVarName = Formatter.formatEnvironmentVariableName;

interface Props {
  onCancelClick?: Option<() => void>,
  onSave: (vars: Array<EnvironmentVariableData>) => void,
  onDelete: (name: string) => void,
  vars: Array<EnvironmentVariableData>,
  errorMessage?: Option<string>,
  focus?: Option<string>,
  onRenderFooter?: Option<(content?, footerClassName?: string) => void>,
  activePanelName: string,
  activePanelIsModal: boolean,
  onToggleActivePanel: (panelName: string, beModal?: boolean, callback?: () => void) => void,
  onClearActivePanel: () => void,
  teamId: string,
  csrfToken: string,
  isAdmin: boolean,
  onAdminLoadedValue: (name: string, value: string) => void
}

interface State {
  vars: Array<EnvironmentVariableData>,
  newVars: Array<EnvironmentVariableData>,
  requestError: Option<string>,
  justSaved: boolean,
  isSaving: boolean,
  isDeleting: boolean,
  deleteVarName: Option<string>,
  adminValuesLoading: Array<string>,
  filter: string
}

class Setter extends React.Component<Props, State> {
  envVarValueInputs: {
    [varName: string]: Option<FocusableTextInputInterface>
  };
  newVarNameInputs: Array<Option<FocusableTextInputInterface>>;
  newVarValueInputs: Array<Option<FocusableTextInputInterface>>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.defaultState();
    this.envVarValueInputs = {};
    this.newVarNameInputs = [];
    this.newVarValueInputs = [];
  }

  getFilter(): string {
    return this.state.filter.trim().toLowerCase();
  }

  updateFilter(newValue: string): void {
    this.setState({
      filter: newValue
    });
  }

  getVars(): Array<EnvironmentVariableData> {
    return this.state.vars;
  }

  getExistingFilteredVars(): Array<EnvironmentVariableData> {
    return this.getFilteredVars(this.getVars());
  }

  getFilteredVars(vars: Array<EnvironmentVariableData>): Array<EnvironmentVariableData> {
    const filter = this.getFilter();
    if (filter) {
      return vars.filter((ea) => {
        const lowerCased = ea.name.toLowerCase();
        return lowerCased.includes(filter) ||
          lowerCased.replace(/_/g, "").includes(filter);
      });
    } else {
      return vars;
    }
  }

  createNewVar(optionalName?: Option<string>): EnvironmentVariableData {
    return {
      name: optionalName || "",
      value: "",
      isAlreadySavedWithValue: false
    };
  }

  defaultState(): State {
    return {
      vars: this.props.vars,
      newVars: [],
      requestError: null,
      justSaved: false,
      isSaving: false,
      isDeleting: false,
      deleteVarName: null,
      adminValuesLoading: [],
      filter: ""
    };
  }

  componentDidMount(): void {
    if (this.props.focus) {
      this.focusOrCreateVarName(this.props.focus);
    }
  }

  componentWillReceiveProps(newProps: Props): void {
    if (newProps.vars !== this.props.vars) {
      this.setState({
        vars: this.state.vars.map((oldVar) => {
          const newVar = newProps.vars.find((ea) => ea.name === oldVar.name);
          if (newVar && newVar.value !== oldVar.value) {
            return newVar;
          } else {
            return oldVar;
          }
        })
      });
    }
  }

  reset(): void {
    this.setState(this.defaultState());
  }

  isValid(): boolean {
    return this.hasChanges() && this.getDuplicateNames().length === 0;
  }

  hasChanges(): boolean {
    return this.hasChangesComparedTo(this.props.vars) || this.getNewVars().some((ea) => !!ea.name);
  }

  hasChangesComparedTo(oldVars: Array<EnvironmentVariableData>): boolean {
    return !this.getVars().every((currentVar, index) => {
      var oldVar = oldVars[index];
      return oldVar &&
        currentVar.name === oldVar.name &&
        currentVar.value === oldVar.value &&
        currentVar.isAlreadySavedWithValue === oldVar.isAlreadySavedWithValue;
    });
  }

  getNewVars(): Array<EnvironmentVariableData> {
    return this.state.newVars;
  }

  setNewVarIndexName(index: number, newName: string): void {
    var previousNewVars = this.getNewVars();
    var newVar = Object.assign({}, previousNewVars[index], {name: formatEnvVarName(newName)});
    var newNewVars = ImmutableObjectUtils.arrayWithNewElementAtIndex(previousNewVars, newVar, index);
    this.setState({newVars: newNewVars});
  }

  setNewVarIndexValue(index: number, newValue: string): void {
    var previousNewVars = this.state.newVars;
    var newVar = Object.assign({}, previousNewVars[index], {value: newValue});
    var newNewVars = ImmutableObjectUtils.arrayWithNewElementAtIndex(previousNewVars, newVar, index);
    this.setState({newVars: newNewVars});
  }

  getDuplicateNames(): Array<string> {
    return this.getNewVars().filter((newVar) => {
      return newVar.name && this.getVars().some((ea) => ea.name === newVar.name);
    }).map((dupe) => dupe.name);
  }

  addNewVar(optionalName?: Option<string>): void {
    this.setState({
      newVars: this.state.newVars.concat(this.createNewVar(optionalName))
    }, () => {
      const newIndex = this.state.newVars.length - 1;
      const newInput = optionalName ? this.newVarValueInputs[newIndex] : this.newVarNameInputs[newIndex];
      if (newInput) {
        newInput.focus();
      }
    });
  }

  focusOrCreateVarName(name: string): void {
    this.setState({
      filter: name
    }, () => {
      const existingVarValueInput = this.envVarValueInputs[name];
      if (existingVarValueInput) {
        existingVarValueInput.focus();
      } else {
        this.addNewVar(name);
      }
    });
  }

  cancelShouldBeDisabled(): boolean {
    return !this.props.onCancelClick && !this.hasChanges();
  }

  onCancel(): void {
    this.setState(this.defaultState());
    if (this.props.onCancelClick) {
      this.props.onCancelClick();
    }
  }

  onChangeVarValue(index: number, newValue: string) {
    var vars = this.getVars();
    var newVar = Object.assign({}, vars[index], {value: newValue});
    this.setState({
      vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
    });
  }

  onSave(): void {
    var namedNewVars = this.getNewVars().filter((ea) => !!ea.name);
    this.setState({
      vars: this.getVars().concat(namedNewVars),
      newVars: [this.createNewVar()],
      requestError: null,
      justSaved: false,
      isSaving: true
    }, () => {
      this.props.onSave(this.state.vars);
    });
  }

  onDelete(name: string): void {
    this.setState({
      isDeleting: true,
      requestError: null
    }, () => {
      DataRequest.jsonPost(jsRoutes.controllers.web.settings.EnvironmentVariablesController.delete().url, {
        teamId: this.props.teamId,
        name: name
      }, this.props.csrfToken).then(() => {
        this.setState({
          deleteVarName: null,
          isDeleting: false,
          vars: this.state.vars.filter((ea) => ea.name !== name)
        }, () => {
          this.props.onDelete(name);
          this.props.onClearActivePanel()
        })
      })
    })
  }

  resetVar(index: number): void {
    var vars = this.getVars();
    var newVar = Object.assign({}, vars[index], {
      isAlreadySavedWithValue: false,
      value: ''
    });
    this.setState({
      vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
    }, () => {
      const input = this.envVarValueInputs[index];
      if (input) {
        input.focus();
      }
    });
  }

  adminLoadValueFor(v: EnvironmentVariableData): void {
    if (this.props.isAdmin) {
      const url = jsRoutes.controllers.web.settings.EnvironmentVariablesController.adminLoadValue(this.props.teamId, v.name).url;
      this.setState({
        requestError: null,
        adminValuesLoading: this.state.adminValuesLoading.concat(v.name)
      }, () => {
        const newAdminValuesLoading = this.state.adminValuesLoading.filter((ea) => ea !== v.name);
        const newState = {
          adminValuesLoading: newAdminValuesLoading
        };
        DataRequest.jsonGet(url).then((json: EnvironmentVariablesData) => {
          const newVar = json.variables ? json.variables[0] : null;
          if (json.error || !newVar) {
            this.setState(Object.assign(newState, {
              requestError: "No variable found with that name. The page may need to be reloaded."
            }));
          } else {
            this.setState(newState, () => {
              this.props.onAdminLoadedValue(newVar.name, newVar.value || "")
            });
          }
        }).catch((err: ResponseError) => {
          this.setState(Object.assign(newState, {
            requestError: `An error occurred while trying to load the variable: ${err.statusText}`
          }));
        });
      });
    }
  }

  adminLoadHandlerFor(v: EnvironmentVariableData): () => void {
    return (() => {
      this.adminLoadValueFor(v);
    });
  }

  isLoadingAdminValueFor(v: EnvironmentVariableData): boolean {
    return this.state.adminValuesLoading.includes(v.name);
  }

  deleteHandlerFor(v: EnvironmentVariableData): () => void {
    return (() => {
      this.setState({
        deleteVarName: v.name
      }, () => {
        this.props.onToggleActivePanel("confirmDeleteEnvVar", true)
      });
    });
  }

  confirmDeleteHandlerFor(name: string): () => void {
    return (() => {
      this.onDelete(name);
    })
  }

  cancelDelete(): void {
    this.props.onClearActivePanel();
    this.setState({
      deleteVarName: null
    });
  }

  getValueInputForVar(envVar: EnvironmentVariableData, index: number) {
    const isLoadingAdminValue = this.isLoadingAdminValueFor(envVar);
    if (envVar.isAlreadySavedWithValue) {
      const value = this.props.isAdmin && envVar.value || "••••••••";
      return (
        <div className="position-relative">
          <div
            className="align-button type-wrap-words type-monospace type-weak type-s mrm"
            title={this.props.isAdmin && envVar.value ? envVar.value : "(Value hidden)"}
          >{value}</div>
          {this.props.isAdmin && !envVar.value ? (
            <DynamicLabelButton
              className="button-s button-shrink mrs mbs"
              onClick={this.adminLoadHandlerFor(envVar)}
              labels={[{
                text: "Reveal…",
                displayWhen: !isLoadingAdminValue
              }, {
                text: "Loading",
                displayWhen: isLoadingAdminValue
              }]}
              disabledWhen={this.state.adminValuesLoading.length > 0}
            />
          ) : null}
        </div>
      );
    } else {
      return (
        <Textarea
          ref={(el) => this.envVarValueInputs[envVar.name] = el}
          className="type-monospace form-input-borderless form-input-height-auto"
          placeholder="Set value"
          value={envVar.value || ""}
          onChange={this.onChangeVarValue.bind(this, index)}
          rows={this.getRowCountForTextareaValue(envVar.value)}
          title={envVar.value ? "" : "No value set"}
        />
      );
    }
  }

  getRowCountForTextareaValue(value: Option<string>): number {
    return Math.min((value || "").split('\n').length, 5);
  }

  onSaveError(): void {
    this.setState({
      requestError: "An error occured while saving. Please try again.",
      isSaving: false
    });
  }

  onSaveComplete(): void {
    this.setState(Object.assign({}, this.defaultState(), {
      justSaved: true
    }));
  }

  getDuplicateErrorMessage() {
    var names = this.getDuplicateNames();
    if (names.length === 0) {
      return null;
    }
    var errorMessage = (names.length === 1) ?
      `There is already a variable named ${names[0]}.` :
      `These variable names already exist: ${names.join(', ')}`;
    return (
      <span className="mbs type-pink type-s align-button fade-in">{errorMessage}</span>
    );
  }

  renderSaveStatus() {
    if (this.state.requestError) {
      return (
        <span className="mbs type-pink type-bold align-button fade-in">{this.state.requestError}</span>
      );
    } else if (this.state.justSaved) {
      return (
        <span className="mbs type-green align-button fade-in"> — saved successfully</span>
      );
    } else {
      return this.getDuplicateErrorMessage();
    }
  }

  renderSetterActions() {
    return (
      <div className="display-inline-block">
        <button type="button"
          className={"button-primary mrs mbs " + (this.state.isSaving ? "button-activated" : "")}
          disabled={!this.isValid()}
          onClick={this.onSave}
        >
                <span className="button-labels">
                  <span className="button-normal-label">Save</span>
                  <span className="button-activated-label">Saving…</span>
                </span>
        </button>
        <button className="mbs mrl"
          type="button"
          onClick={this.onCancel}
          disabled={this.cancelShouldBeDisabled()}
        >
          Cancel
        </button>
        {this.renderSaveStatus()}
      </div>
    );
  }

  renderFooter() {
    if (this.props.onRenderFooter) {
      return this.props.onRenderFooter((
        <div>
          <Collapsible revealWhen={!this.props.activePanelIsModal}>
            <div className="container container-wide pts border-top">
              <div className="columns">
                <div className="column column-one-quarter" />
                <div className="column column-three-quarters plxxxxl">
                  {this.renderSetterActions()}
                </div>
              </div>
            </div>
          </Collapsible>
          {this.state.deleteVarName ? (
            this.renderConfirmDeletePanel(this.state.deleteVarName)
          ) : null}
        </div>
      ));
    } else {
      return (
        <div className="mtxl">
          {this.renderSetterActions()}
        </div>
      );
    }
  }

  renderConfirmDeletePanel(name: string) {
    return (
      <Collapsible revealWhen={this.props.activePanelName === "confirmDeleteEnvVar"}>
        <ConfirmActionPanel
          confirmText={"Delete"}
          confirmingText={"Deleting"}
          onConfirmClick={this.confirmDeleteHandlerFor(name)}
          onCancelClick={this.cancelDelete}
          isConfirming={this.state.isDeleting}
        >
          <div>
            <span>Are you sure you want to delete the environment variable named </span>
            <b className="type-monospace">{name}</b>
            <span>?</span>
          </div>
        </ConfirmActionPanel>
      </Collapsible>
    );
  }

  render() {
    return (
      <div>
        <p>
          <span>Set environment variables to hold secure information like access keys for other services </span>
          <span>that may be used by multiple skills.</span>
        </p>

        <div>

          <div className="columns columns-elastic mbl">
            <div className="column column-shrink type-label align-form-input">Search:</div>
            <div className="column column-expand">
              <FormSearch className="form-input-borderless type-monospace" onChange={this.updateFilter} value={this.state.filter} />
            </div>
          </div>

          <div>
            {this.getExistingFilteredVars().map((ea, index) => {
              return (
                <div className="border bg-white phs mbm columns" key={`envVar${index}`}>
                  <div className="column column-one-quarter mobile-column-one-half">
                    <div className="align-button type-monospace type-s type-wrap-words" title={ea.name}>
                      <SubstringHighlighter text={ea.name} substring={this.getFilter()} />
                    </div>
                  </div>
                  <div className="column column-three-quarters mobile-column-full">
                    <div className="columns columns-elastic">
                      <div className="column column-expand">
                        {this.getValueInputForVar(ea, index)}
                      </div>
                      <div className="column column-shrink display-nowrap">
                        {ea.isAlreadySavedWithValue ? (
                          <Button className="button-s button-shrink mrs mvs" onClick={this.resetVar.bind(this, index)}>Reset</Button>
                        ) : null}
                        {this.props.onRenderFooter ? (
                          <Button
                            className="button-s button-shrink mvs"
                            onClick={this.deleteHandlerFor(ea)}
                          >Delete…</Button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                </div>
              );
            }, this)}
          </div>
        </div>

        <div>
          <div>
            {this.getNewVars().map((v, index) => {
              return (
                <div className="border bg-white phs mbm columns" key={`newEnvVar${index}`}>
                  <div className="column column-one-quarter mobile-column-one-half">
                    <FormInput
                      ref={(el) => this.newVarNameInputs[index] = el}
                      className="form-input-borderless type-monospace"
                      placeholder="New variable name"
                      value={v.name}
                      onChange={this.setNewVarIndexName.bind(this, index)}
                    />
                  </div>
                  <div className="column column-three-quarters mobile-column-full">
                    <Textarea
                      ref={(el) => this.newVarValueInputs[index] = el}
                      className="type-monospace form-input-borderless form-input-height-auto"
                      placeholder="Set value (optional)"
                      value={v.value || ""}
                      onChange={this.setNewVarIndexValue.bind(this, index)}
                      rows={this.getRowCountForTextareaValue(v.value)}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="mtxl">
          <Button
            className="button-s"
            onClick={this.addNewVar}
          >
            Add new environment variable
          </Button>
        </div>

        {this.renderFooter()}
      </div>
    );
  }
}

export default Setter;
