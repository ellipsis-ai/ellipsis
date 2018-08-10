import * as React from 'react';
import Collapsible from '../../shared_ui/collapsible';
import ImmutableObjectUtils from '../../lib/immutable_object_utils';
import Input from '../../form/input';
import Textarea from '../../form/textarea';
import Formatter from '../../lib/formatter';
import autobind from "../../lib/autobind";
import {EnvironmentVariableData, EnvironmentVariablesData} from "./loader";
import Button from "../../form/button";
import {DataRequest, ResponseError} from "../../lib/data_request";
import DynamicLabelButton from "../../form/dynamic_label_button";

const formatEnvVarName = Formatter.formatEnvironmentVariableName;

interface Props {
  onCancelClick?: Option<() => void>,
  onSave: (vars: Array<EnvironmentVariableData>) => void,
  onDelete: (name: string) => void,
  vars: Array<EnvironmentVariableData>,
  errorMessage?: Option<string>,
  focus?: Option<string>,
  onRenderFooter?: Option<(content?, footerClassName?: string) => void>,
  activePanelIsModal: boolean,
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
  adminValuesLoading: Array<string>
}

class Setter extends React.Component<Props, State> {
  envVarValueInputs: Array<Option<Textarea>>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.defaultState();
    this.envVarValueInputs = [];
  }

  getVars(): Array<EnvironmentVariableData> {
    return this.state.vars;
  }

  createNewVar(): EnvironmentVariableData {
    return {
      name: "",
      value: "",
      isAlreadySavedWithValue: false
    };
  }

  defaultState(): State {
    return {
      vars: this.props.vars,
      newVars: [this.createNewVar()],
      requestError: null,
      justSaved: false,
      isSaving: false,
      isDeleting: false,
      adminValuesLoading: []
    };
  }

  componentDidMount(): void {
    if (this.props.focus) {
      this.focusOnVarName(this.props.focus);
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

  addNewVar(): void {
    this.setState({
      newVars: this.state.newVars.concat(this.createNewVar())
    });
  }

  focusOnVarName(name: string): void {
    var matchingVarIndex = this.getVars().findIndex((ea) => ea.name === name);
    const input = matchingVarIndex >= 0 ? this.envVarValueInputs[matchingVarIndex] : null;
    if (input) {
      input.focus();
    }
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
          isDeleting: false,
          vars: this.state.vars.filter((ea) => ea.name !== name)
        }, () => {
          this.props.onDelete(name);
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
      this.onDelete(v.name);
    });
  }

  getValueInputForVar(v: EnvironmentVariableData, index: number) {
    const isLoadingAdminValue = this.isLoadingAdminValueFor(v);
    if (v.isAlreadySavedWithValue) {
      const value = this.props.isAdmin && v.value || "••••••••";
      return (
        <div className="position-relative">
          <div className="display-inline-block type-wrap-words type-monospace type-weak type-xs mrm">{value}</div>
          {this.props.isAdmin && !v.value ? (
            <DynamicLabelButton
              className="button-s button-shrink mrs"
              onClick={this.adminLoadHandlerFor(v)}
              labels={[{
                text: "Load…",
                displayWhen: !isLoadingAdminValue
              }, {
                text: "Loading",
                displayWhen: isLoadingAdminValue
              }]}
              disabledWhen={this.state.adminValuesLoading.length > 0}
            />
          ) : null}
          <Button className="button-s button-shrink mrs" onClick={this.resetVar.bind(this, index)}>
            Reset
          </Button>
          <DynamicLabelButton
            className="button-s button-shrink"
            onClick={this.deleteHandlerFor(v)}
            labels={[{
              text: "Delete…",
              displayWhen: !this.state.isDeleting
            }, {
              text: "Deleting",
              displayWhen: this.state.isDeleting
            }]}
            disabledWhen={this.state.isDeleting}
          />
        </div>
      );
    } else {
      return (
        <Textarea
          ref={(el) => this.envVarValueInputs[index] = el}
          className="type-monospace form-input-borderless form-input-height-auto"
          placeholder="Set value"
          value={v.value || ""}
          onChange={this.onChangeVarValue.bind(this, index)}
          rows={this.getRowCountForTextareaValue(v.value)}
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
        <Collapsible revealWhen={!this.props.activePanelIsModal}>
          <div className="container pts border-top">
            <div className="columns">
              <div className="column column-one-quarter" />
              <div className="column column-three-quarters plxxxxl">
                {this.renderSetterActions()}
              </div>
            </div>
          </div>
        </Collapsible>
      ));
    } else {
      return (
        <div className="mtxl">
          {this.renderSetterActions()}
        </div>
      );
    }
  }

  render() {
    return (
      <div>
        <p>
          <span>Set environment variables to hold secure information like access keys for other services </span>
          <span>that may be used by multiple skills.</span>
        </p>

        <div className="columns">
          <div>
            {this.getVars().map((v, index) => {
              return (
                <div className="column-row" key={`envVar${index}`}>
                  <div className="column column-one-quarter mobile-column-one-half type-monospace pvxs mobile-pbn">
                    <div className={
                      "type-monospace type-xs type-wrap-words " +
                      (v.isAlreadySavedWithValue ? "" : "align-button")
                    }>
                      {v.name}
                    </div>
                  </div>
                  <div className="column column-three-quarters mobile-column-full pvxs mobile-ptn">
                    {this.getValueInputForVar(v, index)}
                  </div>
                </div>
              );
            }, this)}
          </div>
        </div>

        <hr />

        <div className="columns">
          <div>
            {this.getNewVars().map((v, index) => {
              return (
                <div className="column-row" key={`newEnvVar${index}`}>
                  <div className="column column-one-quarter mobile-column-one-half pvxs mobile-phn">
                    <Input
                      className="form-input-borderless type-monospace"
                      placeholder="New variable name"
                      value={v.name}
                      onChange={this.setNewVarIndexName.bind(this, index)}
                    />
                  </div>
                  <div className="column column-three-quarters mobile-column-full pvxs mobile-ptn">
                    <Textarea
                      className="type-monospace form-input-borderless form-input-height-auto"
                      placeholder="Set team-wide value (optional)"
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

        <div className="align-r mts">
          <button type="button"
            className="button-s"
            onClick={this.addNewVar}
          >
            Add another
          </button>
        </div>

        {this.renderFooter()}
      </div>
    );
  }
}

export default Setter;
