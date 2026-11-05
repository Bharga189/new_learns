import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';

// Representative auth API (provided elsewhere).
abstract class AuthService {
  abstract isLoggedIn(): boolean;
  abstract hasRole(role: string): boolean;
}

/** Functional guard (Angular 15 stable functional router APIs). */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminMatchGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  return auth.hasRole('admin');
};

// Route usage:
// { path: 'settings', canActivate: [authGuard], loadComponent: () => import('./settings.page') }
