import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../services/auth.service';
import { Location } from '@angular/common';
import { MatSnackBar } from '@angular/material';
import {ErrorsnackbarService} from '../../../../services/errorsnackbar.service';
import {TranslateService} from '@ngx-translate/core';

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html',
  styleUrls: ['./password.component.scss']
})
export class PasswordComponent implements OnInit {
  form: FormGroup;

  constructor(
    private authService: AuthService,
    private router: Router,
    private location: Location,
    private snackbar: MatSnackBar,
    private translate: TranslateService,
    private errorSnack: ErrorsnackbarService
  ) { }

  ngOnInit() {
    this.form = new FormGroup({
      'oldPassword': new FormControl('', Validators.required),
      'newPassword': new FormControl('', Validators.compose([Validators.required, Validators.minLength(6), Validators.maxLength(64)])),
      'newPasswordConfirmation': new FormControl('', [this.validatePasswords.bind(this)]),
    }, {
      validators: [this.validatePasswords.bind(this)],
    });
  }

  changePassword() {
    if (this.form.valid) {
      this.authService.changePassword(this.form.get('oldPassword').value, this.form.get('newPassword').value)
        .subscribe(
          () => {
            this.router.navigate(['nodes']);
            this.snackbar.open('Password changed');
          },
          (err) => {
            this.errorSnack.open(err);
          },
        );
    }
  }

  back() {
    this.location.back();
  }

  private validatePasswords() {
    if (this.form) {
      return this.form.get('newPassword').value !== this.form.get('newPasswordConfirmation').value
        ? { invalid: true } : null;
    } else {
      return null;
    }
  }
}
