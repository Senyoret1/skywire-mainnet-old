import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';

import { AuthService } from '../../../services/auth.service';
import { SnackbarService } from '../../../services/snackbar.service';
import { InitialSetupComponent } from './initial-setup/initial-setup.component';
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Login page.
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading = false;

  private subscription: Subscription;

  constructor(
    private authService: AuthService,
    private router: Router,
    private snackbarService: SnackbarService,
    private dialog: MatDialog,
  ) { }

  ngOnInit() {
    this.form = new FormGroup({
      'password': new FormControl('', Validators.required),
    });
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  login() {
    if (!this.form.valid || this.loading) {
      return;
    }

    this.loading = true;
    this.subscription = this.authService.login(this.form.get('password').value).subscribe(
      () => this.onLoginSuccess(),
      err => this.onLoginError(err)
    );
  }

  configure() {
    InitialSetupComponent.openDialog(this.dialog);
  }

  private onLoginSuccess() {
    this.router.navigate(['nodes'], { replaceUrl: true });
  }

  private onLoginError(err: HttpErrorResponse) {
    this.loading = false;
    if (err && err.status === 401) {
      this.snackbarService.showError('login.incorrect-password');
    } else {
      this.snackbarService.showError('common.operation-error');
    }
  }
}
